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
package de.eintosti.buildsystem.storage.codec;

import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import de.eintosti.buildsystem.world.display.WorldDisplayImpl;
import de.eintosti.buildsystem.world.display.WorldFilterImpl;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Fixed entities for the codec golden tests: one with every optional field present and one with the least a stored
 * entry can hold. Every value is constant, so the serialized form is byte-for-byte reproducible.
 */
@NullMarked
final class CodecSamples {

    static final UUID FOLDER_ID = UUID.fromString("0f0f0f0f-0000-4000-8000-000000000001");
    static final UUID PARENT_ID = UUID.fromString("0f0f0f0f-0000-4000-8000-000000000002");
    static final UUID MINIMAL_FOLDER_ID = UUID.fromString("0f0f0f0f-0000-4000-8000-000000000003");
    static final UUID WORLD_ID = UUID.fromString("0a0a0a0a-0000-4000-8000-000000000001");
    static final UUID MINIMAL_WORLD_ID = UUID.fromString("0a0a0a0a-0000-4000-8000-000000000002");
    static final UUID PLAYER_ID = UUID.fromString("0b0b0b0b-0000-4000-8000-000000000001");
    static final UUID MINIMAL_PLAYER_ID = UUID.fromString("0b0b0b0b-0000-4000-8000-000000000002");

    static final Builder ALEX = Builder.of(UUID.fromString("0c0c0c0c-0000-4000-8000-000000000001"), "Alex");
    static final Builder SAM = Builder.of(UUID.fromString("0c0c0c0c-0000-4000-8000-000000000002"), "Sam");

    private static final UUID WORLD_IN_FOLDER = UUID.fromString("0d0d0d0d-0000-4000-8000-000000000001");
    private static final UUID OTHER_WORLD_IN_FOLDER = UUID.fromString("0d0d0d0d-0000-4000-8000-000000000002");

    private CodecSamples() {}

    static FolderImpl fullFolder(WorldContext context) {
        FolderImpl parent = new FolderImpl(
                context,
                PARENT_ID,
                "Maps",
                1_600_000_000_000L,
                TestData.ARCHIVE,
                null,
                ALEX,
                Material.CHEST,
                "-",
                "-",
                List.of(),
                new ArrayList<>());
        FolderImpl folder = new FolderImpl(
                context,
                FOLDER_ID,
                "Lobbies",
                1_700_000_000_000L,
                TestData.ARCHIVE,
                parent,
                ALEX,
                Material.OAK_SIGN,
                "maps.lobby",
                "Hub",
                List.of(WORLD_IN_FOLDER, OTHER_WORLD_IN_FOLDER),
                new ArrayList<>());
        folder.setIconSkullTexture("skull-texture");
        return folder;
    }

    static FolderImpl minimalFolder(WorldContext context) {
        return new FolderImpl(
                context,
                MINIMAL_FOLDER_ID,
                "Empty",
                1_700_000_000_001L,
                TestData.PUBLIC,
                null,
                SAM,
                Material.CHEST,
                "-",
                "-",
                List.of(),
                new ArrayList<>());
    }

    static BuildPlayerImpl fullPlayer() {
        SettingsImpl settings = SettingsImpl.builder()
                .navigatorType(NavigatorType.NEW)
                .designColor(DesignColor.RED)
                .worldDisplay(new WorldDisplayImpl(
                        WorldSort.PROJECT_Z_TO_A, new WorldFilterImpl(WorldFilter.Mode.STARTS_WITH, "lob")))
                .clearInventory(true)
                .disableInteract(true)
                .hidePlayers(true)
                .instantPlaceSigns(true)
                .keepNavigator(true)
                .nightVision(true)
                .noClip(true)
                .placePlants(true)
                .scoreboard(false)
                .slabBreaking(true)
                .spawnTeleport(false)
                .openTrapDoors(true)
                .build();
        BuildPlayerImpl player = new BuildPlayerImpl(PLAYER_ID, settings);
        player.setLogoutLocation(new LogoutLocation("maps:lobby", 1.5, 64.0, -3.25, 90.0f, -10.0f));
        return player;
    }

    static BuildPlayerImpl minimalPlayer() {
        return new BuildPlayerImpl(MINIMAL_PLAYER_ID, new SettingsImpl());
    }

    /**
     * Needs a running mock server with a plugin named {@code Terra}, which the stored generator resolves through.
     */
    static BuildWorldImpl fullWorld(WorldContext context) {
        WorldDataImpl data = WorldDataSchema.create("arena", TestData.FINISHED);
        data.set(WorldDataKey.CUSTOM_SPAWN, "arena:1.5:64.0:-3.5:90.0:0.0");
        data.set(WorldDataKey.PERMISSION, "maps.arena");
        data.set(WorldDataKey.PROJECT, "Arena");
        data.set(WorldDataKey.DIFFICULTY, Difficulty.HARD);
        data.set(WorldDataKey.MATERIAL, Material.GRASS_BLOCK);
        data.set(WorldDataKey.VISIBILITY, Visibility.ADDED_PLAYERS);
        data.set(WorldDataKey.BLOCK_BREAKING, false);
        data.set(WorldDataKey.BLOCK_PLACEMENT, false);
        data.set(WorldDataKey.BLOCK_INTERACTIONS, false);
        data.set(WorldDataKey.BUILDERS_ENABLED, true);
        data.set(WorldDataKey.EXPLOSIONS, false);
        data.set(WorldDataKey.MOB_AI, false);
        data.set(WorldDataKey.PHYSICS, false);
        data.set(PhysicsCategory.values()[0].key(), true);
        data.set(WorldDataKey.TIME_SINCE_BACKUP, 42);
        data.set(WorldDataKey.LAST_EDITED, 1_700_000_000_100L);
        data.set(WorldDataKey.LAST_LOADED, 1_700_000_000_200L);
        data.set(WorldDataKey.LAST_UNLOADED, 1_700_000_000_300L);
        return new BuildWorldImpl(
                context,
                WORLD_ID,
                "arena",
                BuildWorldType.IMPORTED,
                data,
                ALEX,
                List.of(ALEX, SAM),
                1_700_000_000_000L,
                new CustomGeneratorImpl("Terra", "Terra", null),
                null);
    }

    static BuildWorldImpl minimalWorld(WorldContext context) {
        return new BuildWorldImpl(
                context,
                MINIMAL_WORLD_ID,
                "plain",
                BuildWorldType.NORMAL,
                WorldDataSchema.create("plain", TestData.NOT_STARTED),
                null,
                List.of(),
                1_700_000_000_001L,
                null,
                null);
    }

    /**
     * {@return the YAML a storage writes for these entries, keyed as given}
     */
    static String toYaml(Map<String, Map<String, Object>> entries) {
        YamlConfiguration yaml = new YamlConfiguration();
        entries.forEach(yaml::set);
        return yaml.saveToString();
    }

    static ConfigurationSection section(String yaml, String key) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null) {
            throw new IllegalArgumentException("No section " + key);
        }
        return section;
    }

    /**
     * {@return the entry as plain nested maps, with values typed the way the YAML parser types them}
     * Serialized maps go through the same parser first, so an int written as a long still compares equal.
     */
    static Map<String, Object> asMap(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        section.getValues(false)
                .forEach((key, value) ->
                        values.put(key, value instanceof ConfigurationSection nested ? asMap(nested) : value));
        return values;
    }

    static Map<String, Object> reparsed(Map<String, Object> serialized) {
        return asMap(section(toYaml(Map.of("entry", serialized)), "entry"));
    }

    static String resource(String name) {
        try (InputStream in = CodecSamples.class.getResourceAsStream("/codec/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource /codec/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
