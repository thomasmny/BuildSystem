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

import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Starts the plugin on a data folder written by BuildSystem 4.0.0 (see {@code upgrade/README.md}) and checks that every
 * stored value comes back as the 4.0.0 server had it, survives a save and restart unchanged, and that no key of the old
 * files is dropped.
 */
class Upgrade400Test {

    private static final String FIXTURE = "upgrade/4.0.0";
    private static final List<String> WORLDS =
            List.of("lobby", "flatland", "nether_build", "end_build", "void_arena", "secret", "Old_Map-2019");
    private static final int FOLDERS = 4;
    private static final int PLAYERS = 3;

    private static final String ALICE = "11111111-1111-1111-1111-111111111111";
    private static final String BOB = "22222222-2222-2222-2222-222222222222";
    private static final String CAROL = "33333333-3333-3333-3333-333333333333";

    @TempDir
    Path temp;

    @Test
    void valuesSurviveTheUpgradeAndARestart() throws IOException {
        Path fixture = UpgradeServer.resource(FIXTURE);
        Path saved = temp.resolve("saved");

        Map<String, String> upgraded;
        try (UpgradeServer server = start(fixture, "first")) {
            upgraded = Snapshot.of(server);
            Expectations.assertContains(upgraded, expected());
            // Only the game rule clean-up of config version 6 runs, and it backs up the config as 4.0.0 wrote it.
            assertEquals(
                    List.of("config.yml.v5.bak"),
                    List.of(server.dataFolder().toFile().list((dir, name) -> name.endsWith(".bak"))));
            assertEquals(
                    Files.readString(fixture.resolve("config.yml")),
                    Files.readString(server.dataFolder().resolve("config.yml.v5.bak")));
            server.stopAndCopyTo(saved);
        }

        YamlKeys.assertNoKeyLost(fixture, saved, Map.of(), Map.of());
        // The global spawn is read back from spawn.yml by the next start; until then the file is all there is.
        assertEquals(
                "lobby:0.5:65.0:0.5:90.0:0.0",
                YamlConfiguration.loadConfiguration(saved.resolve("spawn.yml").toFile())
                        .getString("spawn"));

        try (UpgradeServer server = start(saved, "second")) {
            assertEquals(upgraded, Snapshot.of(server));
        }
    }

    @Test
    void keysFromANewerVersionAreIgnored() throws IOException {
        Path fixture = UpgradeServer.resource(FIXTURE);
        Map<String, String> plain;
        try (UpgradeServer server = start(fixture, "plain")) {
            plain = Snapshot.of(server);
        }

        Path newer = temp.resolve("newer");
        UpgradeServer.copyTree(fixture, newer);
        addUnknownKeys(newer);

        try (UpgradeServer server = start(newer, "newer")) {
            Map<String, String> withExtras = new TreeMap<>(Snapshot.of(server));
            withExtras.keySet().removeIf(key -> key.contains("future"));
            assertEquals(plain, withExtras);
        }
    }

    private UpgradeServer start(Path data, String name) {
        return UpgradeServer.start(data, temp.resolve("worlds-" + name), WORLDS, FOLDERS, PLAYERS);
    }

    /**
     *
     * Adds keys a later version might write: new fields, new sections, new enum constants.
     *
     */
    static void addUnknownKeys(Path data) throws IOException {
        edit(data, "worlds.yml", yaml -> {
            for (String world : yaml.getConfigurationSection("worlds").getKeys(false)) {
                yaml.set("worlds." + world + ".future-field", "value");
                yaml.set("worlds." + world + ".data.future-flag", true);
                yaml.set("worlds." + world + ".data.physics-exceptions.future-physics", true);
            }
            yaml.set("future-section.key", 1);
        });
        edit(data, "folders.yml", yaml -> {
            for (String folder : yaml.getConfigurationSection("folders").getKeys(false)) {
                yaml.set("folders." + folder + ".future-field", List.of("a", "b"));
            }
        });
        edit(data, "players.yml", yaml -> {
            for (String player : yaml.getConfigurationSection("players").getKeys(false)) {
                yaml.set("players." + player + ".settings.future-setting", true);
                yaml.set("players." + player + ".future-field", "value");
            }
        });
        edit(data, "statuses.yml", yaml -> yaml.set("statuses.finished.future-field", 3));
        edit(data, "categories.yml", yaml -> yaml.set("categories.public.future-field", "x"));
        edit(data, "setup.yml", yaml -> yaml.set("setup.future-icons.something", "STONE"));
        edit(data, "spawn.yml", yaml -> yaml.set("future-field", "x"));
        edit(data, "config.yml", yaml -> yaml.set("settings.future-setting", true));
        edit(data, "messages.yml", yaml -> yaml.set("future_message", "&7Hello"));
    }

    private static void edit(Path data, String file, YamlEdit edit) throws IOException {
        File target = data.resolve(file).toFile();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(target);
        edit.apply(yaml);
        yaml.save(target);
    }

    @FunctionalInterface
    interface YamlEdit {
        void apply(YamlConfiguration yaml);
    }

    private static Map<String, String> expected() {
        Map<String, String> values = new TreeMap<>();

        values.put("world.lobby.uuid", "232aa55e-e4dc-4c89-a9ed-d21c27e1b3bd");
        values.put("world.lobby.type", "NORMAL");
        values.put("world.lobby.creator", ALICE + ",Alice");
        values.put("world.lobby.builders", BOB + ",Bob");
        values.put("world.lobby.creation", "1700000000000");
        values.put("world.lobby.generator", "null");
        values.put("world.lobby.folder", "Projects");
        values.put("world.lobby.data.status", "finished");
        values.put("world.lobby.data.project", "Hub");
        values.put("world.lobby.data.permission", "-");
        values.put("world.lobby.data.difficulty", "EASY");
        values.put("world.lobby.data.material", "GRASS_BLOCK");
        values.put("world.lobby.data.spawn", "0.5;65.0;0.5;90.0;0.0");
        values.put("world.lobby.data.physics", "false");
        values.put("world.lobby.data.explosions", "false");
        values.put("world.lobby.data.mob-ai", "false");
        values.put("world.lobby.data.builders-enabled", "true");
        values.put("world.lobby.data.block-interactions", "false");
        values.put("world.lobby.data.block-breaking", "true");
        values.put("world.lobby.data.block-placement", "true");
        values.put("world.lobby.data.pinned", "true");
        values.put("world.lobby.data.visibility", "EVERYONE");
        values.put("world.lobby.data.time-since-backup", "420");
        values.put("world.lobby.data.last-edited", "1700000010002");
        values.put("world.lobby.data.physics-exceptions.fluid-flow", "true");
        values.put("world.lobby.data.physics-exceptions.leaf-decay", "true");
        values.put("world.lobby.data.physics-exceptions.connections", "false");

        values.put("world.flatland.uuid", "63e2fe2d-2961-46d2-a8f2-039e1b9d106b");
        values.put("world.flatland.type", "FLAT");
        values.put("world.flatland.data.last-loaded", "1700000010100");
        values.put("world.flatland.data.last-unloaded", "1700000010101");
        values.put("world.flatland.creator", BOB + ",Bob");
        values.put("world.flatland.builders", "");
        values.put("world.flatland.folder", "Lobbies");
        values.put("world.flatland.data.status", "in_progress");
        values.put("world.flatland.data.block-breaking", "false");
        values.put("world.flatland.data.block-placement", "false");

        values.put("world.nether_build.uuid", "2184b1ce-c158-43d6-87b1-a0bf293fbd9f");
        values.put("world.nether_build.type", "NETHER");
        values.put("world.nether_build.creator", CAROL + ",Carol");
        values.put("world.nether_build.data.status", "review");
        values.put("world.nether_build.data.material", "NETHERRACK");

        values.put("world.end_build.uuid", "e25fb147-3048-463e-8c38-233660f148f6");
        values.put("world.end_build.type", "END");
        values.put("world.end_build.data.status", "almost_finished");

        values.put("world.void_arena.uuid", "4a88a9ed-e49e-4795-b9b5-d8e24414bfc0");
        values.put("world.void_arena.type", "VOID");
        values.put("world.void_arena.folder", "Archive");
        values.put("world.void_arena.data.status", "archive");
        values.put("world.void_arena.data.difficulty", "HARD");
        values.put("world.void_arena.data.material", "GLASS");

        values.put("world.secret.uuid", "19028348-a3e3-410c-8716-496855fda9e7");
        values.put("world.secret.type", "PRIVATE");
        values.put("world.secret.creator", ALICE + ",Alice");
        values.put("world.secret.builders", BOB + ",Bob");
        values.put("world.secret.folder", "Hidden");
        values.put("world.secret.data.visibility", "ADDED_PLAYERS");
        values.put("world.secret.data.permission", "build.secret");
        values.put("world.secret.data.project", "Secret Project");
        values.put("world.secret.data.material", "PLAYER_HEAD");
        values.put("world.secret.data.icon-skull-texture", "e3RleHR1cmVzOnt9fQ==");

        values.put("world.Old_Map-2019.uuid", "b4dce37b-744e-4297-aa93-2dc8b546d241");
        values.put("world.Old_Map-2019.type", "IMPORTED");
        values.put("world.Old_Map-2019.creator", "null");
        values.put("world.Old_Map-2019.creation", "1791634916000");
        values.put("world.Old_Map-2019.generator", "BuildSystem:void");
        values.put("world.Old_Map-2019.folder", "null");
        values.put("world.Old_Map-2019.data.status", "not_started");
        values.put("world.Old_Map-2019.data.material", "FURNACE");

        values.put("folder.Projects.uuid", "21ee33de-1c1a-4398-8a35-3fc4c0d4cbba");
        values.put("folder.Projects.parent", "null");
        values.put("folder.Projects.category", "public");
        values.put("folder.Projects.creator", ALICE + ",Alice");
        values.put("folder.Projects.creation", "1791634915863");
        values.put("folder.Projects.icon", "BOOKSHELF");
        values.put("folder.Projects.worlds", "232aa55e-e4dc-4c89-a9ed-d21c27e1b3bd");
        values.put("folder.Lobbies.uuid", "1e5103b8-9b6f-4342-8cff-6dda3cf8c467");
        values.put("folder.Lobbies.parent", "Projects");
        values.put("folder.Lobbies.worlds", "63e2fe2d-2961-46d2-a8f2-039e1b9d106b");
        values.put("folder.Archive.uuid", "e772f15b-0510-41bb-b523-515f20198a55");
        values.put("folder.Archive.parent", "Lobbies");
        values.put("folder.Archive.creator", BOB + ",Bob");
        values.put("folder.Archive.worlds", "4a88a9ed-e49e-4795-b9b5-d8e24414bfc0");
        values.put("folder.Hidden.uuid", "75de2c5e-b2a5-45e4-91e7-1e17301f2113");
        values.put("folder.Hidden.category", "private");
        values.put("folder.Hidden.permission", "build.hidden");
        values.put("folder.Hidden.project", "Hidden Project");
        values.put("folder.Hidden.icon", "CHEST");
        values.put("folder.Hidden.worlds", "19028348-a3e3-410c-8716-496855fda9e7");

        values.put("status.finished.display-name", "Done");
        values.put("status.finished.color", "&2");
        values.put("status.finished.built-in", "true");
        values.put("status.review.display-name", "Review");
        values.put("status.review.color", "&d");
        values.put("status.review.icon", "PURPLE_DYE");
        values.put("status.review.order", "7");
        values.put("status.review.building-allowed", "false");
        values.put("status.review.progresses-to", "finished");
        values.put("status.review.built-in", "false");
        values.put("status.not_started.progresses-to", "in_progress");
        values.put("status.archive.building-allowed", "false");

        values.put("category.events.display-name", "Events");
        values.put("category.events.color", "&e");
        values.put("category.events.icon", "FIREWORK_ROCKET");
        values.put("category.events.statuses", "review,finished");
        values.put("category.events.visibilities", "EVERYONE");
        values.put("category.events.slot", "14");
        values.put("category.events.built-in", "false");
        values.put("category.public.statuses", "not_started,in_progress,almost_finished,finished,review");
        values.put("category.private.visibilities", "ADDED_PLAYERS");
        values.put("category.archive.visibilities", "ADDED_PLAYERS;EVERYONE");

        values.put("player." + ALICE + ".navigator-type", "NEW");
        values.put("player." + ALICE + ".glass", "LIGHT_BLUE");
        values.put("player." + ALICE + ".sort", "NAME_A_TO_Z");
        values.put("player." + ALICE + ".filter-mode", "STARTS_WITH");
        values.put("player." + ALICE + ".filter-text", "lo");
        values.put("player." + ALICE + ".slab-breaking", "true");
        values.put("player." + ALICE + ".no-clip", "true");
        values.put("player." + ALICE + ".trapdoors", "true");
        values.put("player." + ALICE + ".night-vision", "true");
        values.put("player." + ALICE + ".scoreboard", "false");
        values.put("player." + ALICE + ".keep-navigator", "true");
        values.put("player." + ALICE + ".instant-place-signs", "true");
        values.put("player." + ALICE + ".hide-players", "true");
        values.put("player." + ALICE + ".place-plants", "true");
        values.put("player." + ALICE + ".clear-inventory", "true");
        values.put("player." + ALICE + ".disable-interact", "true");
        values.put("player." + ALICE + ".spawn-teleport", "false");
        values.put("player." + ALICE + ".logout", "lobby:10.5:70.0:-3.25:45.0:10.0");
        values.put("player." + BOB + ".navigator-type", "OLD");
        values.put("player." + BOB + ".glass", "BLACK");
        values.put("player." + BOB + ".scoreboard", "true");
        values.put("player." + BOB + ".spawn-teleport", "true");
        values.put("player." + BOB + ".logout", "flatland:1.0:4.0:1.0:180.0:-5.0");
        values.put("player." + CAROL + ".glass", "RED");
        values.put("player." + CAROL + ".spawn-teleport", "false");
        values.put("player." + CAROL + ".logout", "null");

        values.put("icon.NORMAL", "OAK_LOG");
        values.put("icon.VOID", "GLASS");
        values.put("icon.FLAT", "GRASS_BLOCK");
        values.put("spawn", "lobby:0.5:65.0:0.5:90.0:0.0");

        values.put("config.version", String.valueOf(ConfigMigrationManager.LATEST_VERSION));
        values.put("config.settings.update-checker", "false");
        values.put("config.settings.scoreboard", "false");
        values.put("config.settings.date-format", "yyyy-MM-dd");
        values.put("config.settings.navigator.item", "COMPASS");
        values.put("config.settings.archive.world-gamemode", "SPECTATOR");
        values.put("config.world.limits.public", "5");
        values.put("config.world.limits.private", "-1");
        values.put("config.world.deletion-blacklist", "[world, world_nether, world_the_end, hub]");
        values.put("config.world.defaults.difficulty", "EASY");
        values.put("config.world.defaults.physics-exceptions.leaf-decay", "true");
        values.put("config.world.unload.time-until-unload", "00:30:00");
        values.put("config.world.backup.max-backups-per-world", "3");
        values.put("config.world.backup.storage", "local");
        values.put("config.folder.override-permissions", "false");

        values.put("message.prefix", "&8[&6MyServer&8]");
        values.put("message.player_join", "&a+ %player%");
        values.put("message.loading_world", "&7Loading %world%, hold on");
        // Removed from 4.1's bundled messages, but an admin's edit is never deleted.
        values.put("message.worlds_world_name", "Name of the world");
        return values;
    }
}
