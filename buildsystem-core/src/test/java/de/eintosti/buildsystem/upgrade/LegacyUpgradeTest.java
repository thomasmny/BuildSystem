/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptomorin.xseries.XGameRule;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import de.eintosti.buildsystem.world.menu.GameRuleEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Upgrades from releases older than the generated fixtures: a {@code worlds.yml} in the 2.27.1 format, and the
 * {@code config.yml} that 2.27.1 and 3.0.2 shipped, edited the way an admin would. Both go through the plugin's own
 * startup, so the config migrations, {@code ConfigService} and the world storage all run.
 */
class LegacyUpgradeTest {

    private static final String ALICE = "11111111-1111-1111-1111-111111111111";
    private static final String BOB = "22222222-2222-2222-2222-222222222222";
    private static final String CAROL = "33333333-3333-3333-3333-333333333333";
    private static final List<String> WORLDS = List.of("Spawn_2019", "Bobs_Plot");
    private static final String WORLDS_BACKUP = "worlds.yml" + StorageMigration.BACKUP_SUFFIX;

    @TempDir
    Path temp;

    @Test
    void worldsFrom2271AreRekeyedOnceAndKeepTheirIds() throws IOException {
        Path fixture = UpgradeServer.resource("upgrade/2.27.1");
        Path saved = temp.resolve("saved");

        Map<String, String> upgraded;
        try (UpgradeServer server = UpgradeServer.start(fixture, temp.resolve("worlds-first"), WORLDS, 0, 0)) {
            upgraded = Snapshot.of(server);
            Map<String, String> expected = new TreeMap<>();
            expected.put("world.Spawn_2019.type", "NORMAL");
            expected.put("world.Spawn_2019.creator", ALICE + ",Alice");
            expected.put("world.Spawn_2019.builders", BOB + ",Bob;" + CAROL + ",Carol");
            expected.put("world.Spawn_2019.creation", "1600000000000");
            expected.put("world.Spawn_2019.data.status", "finished");
            expected.put("world.Spawn_2019.data.project", "Lobby");
            expected.put("world.Spawn_2019.data.material", "GRASS_BLOCK");
            expected.put("world.Spawn_2019.data.visibility", "EVERYONE");
            expected.put("world.Spawn_2019.data.spawn", "4.5;70.0;-2.5;90.0;0.0");
            expected.put("world.Spawn_2019.data.physics", "false");
            expected.put("world.Spawn_2019.data.explosions", "false");
            expected.put("world.Spawn_2019.data.block-interactions", "false");
            expected.put("world.Spawn_2019.data.last-edited", "1600000300000");
            expected.put("world.Bobs_Plot.type", "CUSTOM");
            expected.put("world.Bobs_Plot.creator", BOB + ",Bob");
            expected.put("world.Bobs_Plot.builders", "");
            expected.put("world.Bobs_Plot.data.status", "not_started");
            expected.put("world.Bobs_Plot.data.visibility", "ADDED_PLAYERS");
            expected.put("world.Bobs_Plot.data.permission", "worlds.Bobs_Plot");
            expected.put("world.Bobs_Plot.data.material", "WRITABLE_BOOK");
            // Custom worlds stored only the plugin name, and the plugin is not installed here.
            expected.put("world.Bobs_Plot.generator", "Plugin:Plugin");
            Expectations.assertContains(upgraded, expected);
            server.stopAndCopyTo(saved);
        }

        YamlConfiguration worlds =
                YamlConfiguration.loadConfiguration(saved.resolve("worlds.yml").toFile());
        assertEquals(StorageMigration.CURRENT_VERSION, worlds.getInt("version"));
        ConfigurationSection section = worlds.getConfigurationSection("worlds");
        assertEquals(
                Set.of(upgraded.get("world.Spawn_2019.uuid"), upgraded.get("world.Bobs_Plot.uuid")),
                section.getKeys(false));
        assertEquals("Spawn_2019", section.getString(upgraded.get("world.Spawn_2019.uuid") + ".name"));
        assertEquals(Set.of(WORLDS_BACKUP), backups(saved));
        assertEquals(Files.readString(fixture.resolve("worlds.yml")), Files.readString(saved.resolve(WORLDS_BACKUP)));

        try (UpgradeServer server = UpgradeServer.start(saved, temp.resolve("worlds-second"), WORLDS, 0, 0)) {
            // The ids made up during the migration are the ids from now on.
            assertEquals(upgraded, Snapshot.of(server));
            assertEquals(Set.of(WORLDS_BACKUP), backups(server.dataFolder()));
            assertEquals(
                    Files.readString(fixture.resolve("worlds.yml")),
                    Files.readString(server.dataFolder().resolve(WORLDS_BACKUP)));
        }
    }

    @Test
    void shippedConfigFrom2271KeepsAnAdminsValues() throws IOException {
        Path data = shippedConfig("2.27.1", config -> {
            config.set("settings.archive-world-game-mode", "SPECTATOR");
            config.set("settings.save-from-death.enable", false);
            config.set("world.max-amount.public", 4);
            config.set("world.default.worldborder.size", 5000);
            config.set("world.default.gamerules.doDaylightCycle", true);
            config.set("world.void-block", false);
        });

        try (UpgradeServer server = UpgradeServer.start(data, temp.resolve("worlds"), List.of(), 0, 0)) {
            assertMigratedOnce(server, "config.yml.v1.bak");
            PluginConfig config = server.services().config().current();
            assertEquals(GameMode.SPECTATOR, config.settings().archive().worldGameMode());
            assertEquals(false, config.settings().saveFromDeath().enabled());
            assertEquals(4, config.world().limits().publicWorlds());
            assertEquals(-1, config.world().limits().privateWorlds());
            assertEquals(5000, config.world().defaults().worldBorderSize());
            assertAppliedGameRule(server, "advance_time", true);
            assertAppliedGameRule(server, "spawn_mobs", false);
            assertEquals(false, config.world().voidBlock().enabled());
        }
    }

    @Test
    void shippedConfigFrom302KeepsAnAdminsValues() throws IOException {
        Path data = shippedConfig("3.0.2", config -> {
            config.set("settings.archive.world-gamemode", "CREATIVE");
            config.set("world.max-amount.private", 2);
            config.set("world.default.worldborder.size", 7000);
            config.set("world.default.gamerules.doMobSpawning", true);
            // 3.0.2 shipped disabled-physics twice. It read settings.*, and world.* was a dead copy.
            config.set("settings.disabled-physics.prevent-fluid-flow", false);
            config.set("world.disabled-physics.prevent-fluid-flow", true);
            config.set("settings.disabled-physics.prevent-connections", true);
            config.set("world.disabled-physics.prevent-connections", false);
        });

        try (UpgradeServer server = UpgradeServer.start(data, temp.resolve("worlds"), List.of(), 0, 0)) {
            assertMigratedOnce(server, "config.yml.v1.bak");
            PluginConfig config = server.services().config().current();
            assertEquals(GameMode.CREATIVE, config.settings().archive().worldGameMode());
            assertEquals(2, config.world().limits().privateWorlds());
            assertEquals(-1, config.world().limits().publicWorlds());
            assertEquals(7000, config.world().defaults().worldBorderSize());
            assertAppliedGameRule(server, "spawn_mobs", true);
            assertAppliedGameRule(server, "advance_time", false);
            PluginConfig.World.Defaults defaults = config.world().defaults();
            assertTrue(defaults.physicsException(PhysicsCategory.FLUID_FLOW));
            assertEquals(false, defaults.physicsException(PhysicsCategory.CONNECTIONS));
            assertEquals(false, defaults.physicsException(PhysicsCategory.FALLING_BLOCKS));
        }
    }

    @Test
    void gameRuleChangedOn40WinsOverItsOld3xName() throws IOException {
        // A 3.x config saved by 4.0 holds the old name next to the shipped new one, and the admin then changed the new
        // one on 4.0.
        Path data = temp.resolve("data-4.0");
        Files.createDirectories(data);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                UpgradeServer.resource("upgrade/4.0.0/config.yml").toFile());
        config.set("world.defaults.gamerules.doDaylightCycle", false);
        config.set("world.defaults.gamerules.advance_time", true);
        config.set("world.defaults.gamerules.doMobSpawning", true);
        config.save(data.resolve("config.yml").toFile());

        try (UpgradeServer server = UpgradeServer.start(data, temp.resolve("worlds"), List.of(), 0, 0)) {
            assertMigratedOnce(server, "config.yml.v5.bak");
            assertAppliedGameRule(server, "advance_time", true);
            // spawn_mobs still had the shipped value, so the old name's value is the admin's.
            assertAppliedGameRule(server, "spawn_mobs", true);
        }
    }

    private Path shippedConfig(String version, Upgrade400Test.YamlEdit edit) throws IOException {
        Path data = temp.resolve("data-" + version);
        Files.createDirectories(data);
        File target = data.resolve("config.yml").toFile();
        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(UpgradeServer.resource("upgrade/shipped/config-" + version + ".yml")
                        .toFile());
        edit.apply(config);
        config.save(target);
        return data;
    }

    private static void assertMigratedOnce(UpgradeServer server, String backup) {
        assertEquals(
                ConfigMigrationManager.LATEST_VERSION,
                server.plugin().getConfig().getInt("version"));
        assertEquals(Set.of(backup), backups(server.dataFolder()));
    }

    /**
     * Asserts the value a new world gets for a game rule. Worlds get the configured rules applied in order, so the last
     * entry for a rule is the one that sticks. The old name must also be gone from the saved config.
     */
    private static void assertAppliedGameRule(UpgradeServer server, String name, Object value) {
        XGameRule<?> rule = XGameRule.of(name).orElseThrow();
        Object applied = null;
        for (GameRuleEntry<?> entry :
                server.services().config().current().world().defaults().gameRules()) {
            if (entry.rule() == rule) {
                applied = entry.value();
            }
        }
        assertEquals(value, applied, name);
        Set<String> names = server.plugin()
                .getConfig()
                .getConfigurationSection("world.defaults.gamerules")
                .getKeys(false);
        names.remove(name);
        names.forEach(other -> assertNotEquals(Optional.of(rule), XGameRule.of(other), other + " duplicates " + name));
    }

    private static Set<String> backups(Path folder) {
        return new TreeSet<>(List.of(folder.toFile().list((dir, name) -> name.endsWith(".bak"))));
    }
}
