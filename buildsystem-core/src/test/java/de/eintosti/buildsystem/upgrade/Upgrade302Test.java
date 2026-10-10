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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Starts the plugin on a data folder written by BuildSystem 3.0.2 (see {@code upgrade/README.md}). 3.0.2 keyed worlds
 * and folders by name, stored statuses as enum names, marked private worlds with a boolean and kept an older config
 * layout, so this exercises every one-time migration: they must run once, keep every value, and leave a backup.
 */
class Upgrade302Test {

    private static final String FIXTURE = "upgrade/3.0.2";
    private static final List<String> WORLDS =
            List.of("lobby", "flatland", "nether_build", "end_build", "void_arena", "secret", "Old_Map-2019");
    private static final int FOLDERS = 4;
    private static final int PLAYERS = 3;

    private static final String ALICE = "11111111-1111-1111-1111-111111111111";
    private static final String BOB = "22222222-2222-2222-2222-222222222222";
    private static final String CAROL = "33333333-3333-3333-3333-333333333333";
    private static final long T0 = 1_735_689_600_000L;

    /**
     *
     * Where each 3.0.2 config key lives after the config migrations, longest prefix first.
     *
     */
    private static final Map<String, String> CONFIG_MOVES = new LinkedHashMap<>();

    static {
        CONFIG_MOVES.put("messages.", "settings.");
        CONFIG_MOVES.put("world.default.worldborder.size", "world.defaults.worldborder-size");
        CONFIG_MOVES.put("world.default.gamerules.doDaylightCycle", "world.defaults.gamerules.advance_time");
        CONFIG_MOVES.put("world.default.gamerules.doMobSpawning", "world.defaults.gamerules.spawn_mobs");
        CONFIG_MOVES.put("world.default.settings.", "world.defaults.");
        CONFIG_MOVES.put("world.default.", "world.defaults.");
        CONFIG_MOVES.put("world.import-all.delay", "world.import-all-delay");
        CONFIG_MOVES.put("world.max-amount.", "world.limits.");
        CONFIG_MOVES.put("settings.disabled-physics.prevent-", "world.defaults.physics-exceptions.");
        CONFIG_MOVES.put("world.disabled-physics.prevent-", "world.defaults.physics-exceptions.");
        CONFIG_MOVES.put("world.backup.storage.type", "world.backup.storage");
        CONFIG_MOVES.put("world.backup.storage.", "storage.");
    }

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
            server.stopAndCopyTo(saved);
        }

        // Each one-time migration keeps the file as 3.0.2 wrote it.
        for (String backup : List.of(
                "config.yml.v2.bak",
                "worlds.yml" + StorageMigration.BACKUP_SUFFIX,
                "folders.yml" + StorageMigration.BACKUP_SUFFIX)) {
            assertTrue(Files.exists(saved.resolve(backup)), backup);
            String original = backup.substring(0, backup.indexOf(".yml") + 4);
            assertEquals(Files.readString(fixture.resolve(original)), Files.readString(saved.resolve(backup)));
        }

        Map<String, String> folderIds = new HashMap<>();
        upgraded.forEach((key, value) -> {
            if (key.startsWith("folder.") && key.endsWith(".uuid")) {
                folderIds.put(key.substring("folder.".length(), key.length() - ".uuid".length()), value);
            }
        });
        YamlKeys.assertNoKeyLost(
                fixture,
                saved,
                Map.of(
                        "worlds.yml", rekey("worlds.", worldIds(fixture)),
                        "folders.yml", rekey("folders.", folderIds),
                        "config.yml", Upgrade302Test::movedConfigKey),
                Map.of(
                        // Replaced by data.visibility.
                        "worlds.yml", privateFlags(fixture),
                        // MigrationV4ToV5 keeps only the path of the backend in use, and local has none.
                        "config.yml", Set.of("storage.s3.path", "storage.sftp.path")));

        Set<String> backups = backups(saved);
        try (UpgradeServer server = start(saved, "second")) {
            assertEquals(upgraded, Snapshot.of(server));
            // Nothing migrates a second time.
            assertEquals(backups, backups(server.dataFolder()));
        }
    }

    @Test
    void keysFromANewerVersionAreIgnored() throws IOException {
        Path fixture = UpgradeServer.resource(FIXTURE);
        Path once = temp.resolve("once");
        Map<String, String> plain;
        try (UpgradeServer server = start(fixture, "plain")) {
            plain = Snapshot.of(server);
            server.stopAndCopyTo(once);
        }

        Upgrade400Test.addUnknownKeys(once);

        try (UpgradeServer server = start(once, "newer")) {
            Map<String, String> withExtras = new TreeMap<>(Snapshot.of(server));
            withExtras.keySet().removeIf(key -> key.contains("future"));
            assertEquals(plain, withExtras);
        }
    }

    private UpgradeServer start(Path data, String name) {
        return UpgradeServer.start(data, temp.resolve("worlds-" + name), WORLDS, FOLDERS, PLAYERS);
    }

    private static String movedConfigKey(String key) {
        for (Map.Entry<String, String> move : CONFIG_MOVES.entrySet()) {
            if (key.startsWith(move.getKey())) {
                return move.getValue() + key.substring(move.getKey().length());
            }
        }
        return key;
    }

    private static UnaryOperator<String> rekey(String root, Map<String, String> ids) {
        return key -> {
            String rest = key.substring(root.length());
            int dot = rest.indexOf('.');
            String name = dot < 0 ? rest : rest.substring(0, dot);
            String id = ids.get(name);
            return id == null ? key : root + id + rest.substring(name.length());
        };
    }

    private static Map<String, String> worldIds(Path fixture) {
        ConfigurationSection worlds = YamlConfiguration.loadConfiguration(
                        fixture.resolve("worlds.yml").toFile())
                .getConfigurationSection("worlds");
        Map<String, String> ids = new HashMap<>();
        for (String name : worlds.getKeys(false)) {
            ids.put(name, worlds.getString(name + ".uuid"));
        }
        return ids;
    }

    private static Set<String> privateFlags(Path fixture) {
        Set<String> keys = new TreeSet<>();
        worldIds(fixture).values().forEach(id -> keys.add("worlds." + id + ".data.private"));
        return keys;
    }

    private static Set<String> backups(Path folder) {
        return new TreeSet<>(List.of(folder.toFile().list((dir, name) -> name.endsWith(".bak"))));
    }

    private static Map<String, String> expected() {
        Map<String, String> values = new TreeMap<>();

        values.put("world.lobby.uuid", "0e7a1aca-d915-4fb9-84b1-30f8b95cc373");
        values.put("world.lobby.type", "NORMAL");
        values.put("world.lobby.creator", ALICE + ",Alice");
        values.put("world.lobby.builders", BOB + ",Bob");
        values.put("world.lobby.creation", String.valueOf(T0));
        values.put("world.lobby.folder", "Projects");
        values.put("world.lobby.data.status", "finished");
        values.put("world.lobby.data.project", "Hub");
        values.put("world.lobby.data.difficulty", "EASY");
        values.put("world.lobby.data.material", "GRASS_BLOCK");
        // 3.0.2 wrote the custom spawn under data but read it from the top level, so 3.0.2 itself lost it on restart.
        values.put("world.lobby.data.spawn", "12.5;66.0;-7.5;180.0;15.0");
        values.put("world.lobby.data.physics", "false");
        values.put("world.lobby.data.explosions", "false");
        values.put("world.lobby.data.mob-ai", "false");
        values.put("world.lobby.data.builders-enabled", "true");
        values.put("world.lobby.data.block-interactions", "false");
        values.put("world.lobby.data.time-since-backup", "420");
        values.put("world.lobby.data.visibility", "EVERYONE");
        values.put("world.lobby.data.last-edited", String.valueOf(T0 + 300_000));

        values.put("world.flatland.uuid", "22b87873-4980-47ca-9423-dfd9def7580b");
        values.put("world.flatland.type", "FLAT");
        values.put("world.flatland.creator", BOB + ",Bob");
        values.put("world.flatland.folder", "Lobbies");
        values.put("world.flatland.data.status", "in_progress");
        values.put("world.flatland.data.last-loaded", String.valueOf(T0 + 100_001));
        values.put("world.flatland.data.last-unloaded", String.valueOf(T0 + 200_001));
        values.put("world.flatland.data.block-breaking", "false");
        values.put("world.flatland.data.block-placement", "false");

        values.put("world.nether_build.uuid", "39f399fb-a94a-4df6-86bf-89e37078f727");
        values.put("world.nether_build.type", "NETHER");
        values.put("world.nether_build.data.status", "almost_finished");
        values.put("world.nether_build.data.difficulty", "HARD");

        values.put("world.end_build.uuid", "b5104aed-5a73-4988-a749-427d4e3bed70");
        values.put("world.end_build.type", "END");
        values.put("world.end_build.data.status", "archive");

        values.put("world.void_arena.uuid", "7c4f90af-19fe-4b21-8428-31a8798c3e50");
        values.put("world.void_arena.type", "VOID");
        values.put("world.void_arena.folder", "Archive");
        values.put("world.void_arena.data.status", "hidden");
        values.put("world.void_arena.data.material", "GLASS");

        values.put("world.secret.uuid", "c0fdcd74-7e86-402b-86a2-86e882e54b35");
        values.put("world.secret.creator", ALICE + ",Alice");
        values.put("world.secret.builders", BOB + ",Bob");
        values.put("world.secret.folder", "Hidden");
        values.put("world.secret.data.visibility", "ADDED_PLAYERS");
        values.put("world.secret.data.permission", "build.secret");
        values.put("world.secret.data.project", "Secret Project");
        values.put("world.secret.data.status", "in_progress");

        values.put("world.Old_Map-2019.uuid", "bc251dc5-6be3-4cb7-8fca-a093c02adc45");
        values.put("world.Old_Map-2019.type", "IMPORTED");
        values.put("world.Old_Map-2019.creator", "null");
        values.put("world.Old_Map-2019.creation", String.valueOf(T0 + 6000));
        values.put("world.Old_Map-2019.generator", "BuildSystem:VOID");
        values.put("world.Old_Map-2019.data.status", "not_started");
        values.put("world.Old_Map-2019.data.material", "FURNACE");

        values.put("folder.Projects.parent", "null");
        values.put("folder.Projects.category", "public");
        values.put("folder.Projects.creator", ALICE + ",Alice");
        values.put("folder.Projects.worlds", "0e7a1aca-d915-4fb9-84b1-30f8b95cc373");
        values.put("folder.Lobbies.parent", "Projects");
        values.put("folder.Lobbies.worlds", "22b87873-4980-47ca-9423-dfd9def7580b");
        values.put("folder.Archive.parent", "Lobbies");
        values.put("folder.Archive.creator", BOB + ",Bob");
        values.put("folder.Archive.worlds", "7c4f90af-19fe-4b21-8428-31a8798c3e50");
        values.put("folder.Hidden.parent", "null");
        values.put("folder.Hidden.category", "private");
        values.put("folder.Hidden.icon", "ENDER_CHEST");
        values.put("folder.Hidden.permission", "build.hidden");
        values.put("folder.Hidden.project", "Hidden Project");
        values.put("folder.Hidden.worlds", "c0fdcd74-7e86-402b-86a2-86e882e54b35");

        // The built-in statuses take their names from 3.0.2's status_<id> messages and their icons from setup.yml.
        values.put("status.finished.display-name", "Done");
        values.put("status.finished.color", "&5");
        values.put("status.finished.icon", "EMERALD_BLOCK");
        values.put("status.not_started.display-name", "Not Started");
        values.put("status.not_started.icon", "RED_DYE");
        values.put("status.archive.building-allowed", "false");

        values.put("player." + ALICE + ".navigator-type", "NEW");
        values.put("player." + ALICE + ".glass", "PURPLE");
        values.put("player." + ALICE + ".sort", "NAME_A_TO_Z");
        values.put("player." + ALICE + ".filter-mode", "CONTAINS");
        values.put("player." + ALICE + ".filter-text", "lob");
        values.put("player." + ALICE + ".slab-breaking", "true");
        values.put("player." + ALICE + ".no-clip", "true");
        values.put("player." + ALICE + ".trapdoors", "true");
        values.put("player." + ALICE + ".night-vision", "true");
        values.put("player." + ALICE + ".instant-place-signs", "true");
        values.put("player." + ALICE + ".hide-players", "true");
        values.put("player." + ALICE + ".scoreboard", "false");
        values.put("player." + ALICE + ".logout", "lobby:10.5:70.0:-3.5:45.0:10.0");
        values.put("player." + BOB + ".navigator-type", "OLD");
        values.put("player." + BOB + ".glass", "BLACK");
        values.put("player." + BOB + ".logout", "flatland:1.5:-60.0:2.5:0.0:0.0");
        values.put("player." + CAROL + ".keep-navigator", "true");
        values.put("player." + CAROL + ".place-plants", "true");
        values.put("player." + CAROL + ".spawn-teleport", "false");
        values.put("player." + CAROL + ".logout", "null");

        values.put("icon.NORMAL", "OAK_LOG");
        values.put("spawn", "lobby:0.5:65.0:0.5:90.0:0.0");

        values.put("config.version", String.valueOf(ConfigMigrationManager.LATEST_VERSION));
        values.put("config.settings.date-format", "yyyy-MM-dd");
        values.put("config.settings.join-quit-messages", "false");
        values.put("config.settings.spawn-teleport-message", "true");
        values.put("config.settings.navigator.item", "COMPASS");
        values.put("config.settings.scoreboard", "false");
        values.put("config.settings.update-checker", "false");
        // prevent-fluid-flow: false becomes the fluid-flow exception.
        values.put("config.world.defaults.physics-exceptions.fluid-flow", "true");
        values.put("config.world.defaults.physics-exceptions.connections", "false");
        values.put("config.world.defaults.physics-exceptions.falling-blocks", "false");
        values.put("config.world.defaults.difficulty", "EASY");
        values.put("config.world.defaults.time.noon", "5000");
        values.put("config.world.defaults.worldborder-size", "5000");
        values.put("config.world.defaults.explosions", "false");
        values.put("config.world.defaults.builders-enabled.public", "true");
        // Renamed to the names the config ships with.
        values.put("config.world.defaults.gamerules.advance_time", "false");
        values.put("config.world.defaults.gamerules.spawn_mobs", "false");
        values.put("config.world.import-all-delay", "10");
        values.put("config.world.limits.private", "3");
        values.put("config.world.limits.public", "-1");
        values.put("config.world.lock-weather", "false");
        values.put("config.world.unload.time-until-unload", "00:30:00");
        values.put("config.world.backup.max-backups-per-world", "7");
        values.put("config.world.backup.storage", "local");
        values.put("config.storage.sftp.host", "backup.example.org");
        values.put("config.storage.sftp.port", "2222");
        values.put("config.folder.override-permissions", "false");

        values.put("message.prefix", "&8▎ &6MyServer &8»");
        values.put("message.player_join", "&7[&a+&7] &e%player% joined");
        values.put("message.status_finished", "&5Done");
        values.put("message.worlds_world_name", "Map name");
        return values;
    }
}
