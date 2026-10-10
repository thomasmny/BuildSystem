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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Pins the stored world format: a file written by 4.0 loads with the same values and writes back the same keys, and
 * keys missing from older files fall back to the same defaults, several of which differ from a new world's.
 */
@NullMarked
class WorldCodecTest {

    private final WorldContext context = TestData.worldContext();
    private final PlayerLookupService playerLookup = mock(PlayerLookupService.class);
    private final WorldCodec codec = new WorldCodec(context, playerLookup);

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        // The stored chunk generator only resolves when its plugin is installed.
        MockBukkit.createMockPlugin("Terra");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void fileWrittenBy40_writesBackTheSameValues() {
        String yaml = CodecSamples.resource("worlds-4.0.yml");
        for (String key : List.of(CodecSamples.WORLD_ID.toString(), CodecSamples.MINIMAL_WORLD_ID.toString())) {
            ConfigurationSection section = CodecSamples.section(yaml, key);

            BuildWorldImpl world = codec.deserialize(key, section);

            assertEquals(CodecSamples.asMap(section), CodecSamples.reparsed(codec.serialize(world)), key);
        }
    }

    @Test
    void fileWrittenBy40_loadsEveryField() {
        String key = CodecSamples.WORLD_ID.toString();

        BuildWorldImpl world =
                codec.deserialize(key, CodecSamples.section(CodecSamples.resource("worlds-4.0.yml"), key));

        assertEquals(CodecSamples.WORLD_ID, world.getUniqueId());
        assertEquals("arena", world.getName());
        assertEquals(BuildWorldType.IMPORTED, world.getType());
        assertEquals(1_700_000_000_000L, world.getCreation());
        assertEquals(
                CodecSamples.ALEX.toString(), String.valueOf(world.getBuilders().getCreator()));
        assertEquals(2, world.getBuilders().getAllBuilders().size());
        assertEquals("Terra:Terra", String.valueOf(world.getCustomGenerator()));
        WorldDataImpl data = (WorldDataImpl) world.getData();
        assertEquals(TestData.FINISHED, data.get(WorldDataKey.STATUS));
        assertEquals("arena:1.5:64.0:-3.5:90.0:0.0", data.get(WorldDataKey.CUSTOM_SPAWN));
        assertEquals("maps.arena", data.get(WorldDataKey.PERMISSION));
        assertEquals("Arena", data.get(WorldDataKey.PROJECT));
        assertEquals(Difficulty.HARD, data.get(WorldDataKey.DIFFICULTY));
        assertEquals(Visibility.ADDED_PLAYERS, data.get(WorldDataKey.VISIBILITY));
        assertFalse(data.get(WorldDataKey.BLOCK_BREAKING));
        assertTrue(data.get(WorldDataKey.BUILDERS_ENABLED));
        assertTrue(data.get(PhysicsCategory.BLOCK_UPDATES.key()));
        assertEquals(42, data.get(WorldDataKey.TIME_SINCE_BACKUP));
        assertEquals(1_700_000_000_300L, data.get(WorldDataKey.LAST_UNLOADED));
    }

    @Test
    void sparseWorld_fallsBackToLegacyDefaults() {
        when(context.configService().current().world().defaults().physicsException(PhysicsCategory.LEAF_DECAY))
                .thenReturn(true);

        BuildWorldImpl world = load("""
                data:
                  block-breaking: false
                """);

        assertEquals("stored", world.getName());
        assertEquals(BuildWorldType.UNKNOWN, world.getType());
        assertEquals(-1L, world.getCreation());
        assertNull(world.getBuilders().getCreator());
        assertTrue(world.getBuilders().getAllBuilders().isEmpty());
        assertNull(world.getCustomGenerator());
        WorldDataImpl data = (WorldDataImpl) world.getData();
        assertEquals(context.statusRegistry().getDefault(), data.get(WorldDataKey.STATUS));
        assertEquals(Material.BEDROCK, data.get(WorldDataKey.MATERIAL), "not the GRASS_BLOCK a new world gets");
        assertEquals(Difficulty.PEACEFUL, data.get(WorldDataKey.DIFFICULTY));
        assertEquals(Visibility.EVERYONE, data.get(WorldDataKey.VISIBILITY));
        assertEquals("", data.get(WorldDataKey.CUSTOM_SPAWN));
        assertEquals("-", data.get(WorldDataKey.PERMISSION));
        assertFalse(data.get(WorldDataKey.BLOCK_BREAKING));
        assertTrue(data.get(WorldDataKey.BLOCK_PLACEMENT));
        assertTrue(data.get(PhysicsCategory.LEAF_DECAY.key()), "an absent exception takes the configured default");
        assertFalse(data.get(PhysicsCategory.GROWTH.key()));
        assertEquals(-1L, data.get(WorldDataKey.LAST_EDITED));
    }

    @Test
    void preV4World_migratesLegacyKeys() {
        UUID creatorId = UUID.fromString("0c0c0c0c-0000-4000-8000-000000000009");
        BuildWorldImpl world = load("""
                name: old
                creator: Alex
                creator-id: %s
                spawn: old:1.0:2.0:3.0:0.0:0.0
                data:
                  status: ALMOST_FINISHED
                  private: true
                  difficulty: easy
                  material: not_a_material
                """.formatted(creatorId));

        assertEquals("old", world.getName());
        Builder creator = Objects.requireNonNull(world.getBuilders().getCreator());
        assertEquals(creatorId, creator.getUniqueId());
        assertEquals("Alex", creator.getName());
        WorldDataImpl data = (WorldDataImpl) world.getData();
        assertEquals(TestData.ALMOST_FINISHED, data.get(WorldDataKey.STATUS));
        assertEquals(Visibility.ADDED_PLAYERS, data.get(WorldDataKey.VISIBILITY));
        assertEquals(Difficulty.EASY, data.get(WorldDataKey.DIFFICULTY));
        assertEquals(Material.BEDROCK, data.get(WorldDataKey.MATERIAL));
        assertEquals("old:1.0:2.0:3.0:0.0:0.0", data.get(WorldDataKey.CUSTOM_SPAWN));
    }

    @Test
    void preV4CreatorWithoutId_isLookedUpByName() {
        UUID creatorId = UUID.fromString("0c0c0c0c-0000-4000-8000-000000000009");
        when(playerLookup.lookupUniqueIdBlocking("Alex")).thenReturn(creatorId);

        BuildWorldImpl world = load("""
                creator: Alex
                creator-id: "null"
                """);

        assertEquals(
                creatorId,
                Objects.requireNonNull(world.getBuilders().getCreator()).getUniqueId());
    }

    @Test
    void unknownEnums_fallBackToDefaults() {
        BuildWorldImpl world = load("""
                type: FLAT_EARTH
                data:
                  status: someday
                  difficulty: nightmare
                  visibility: SOMETIMES
                """);

        assertEquals(BuildWorldType.UNKNOWN, world.getType());
        WorldDataImpl data = (WorldDataImpl) world.getData();
        assertEquals(context.statusRegistry().getDefault(), data.get(WorldDataKey.STATUS));
        assertEquals(Difficulty.PEACEFUL, data.get(WorldDataKey.DIFFICULTY));
        assertEquals(Visibility.EVERYONE, data.get(WorldDataKey.VISIBILITY));
    }

    private BuildWorldImpl load(String body) {
        String key = UUID.fromString("0a0a0a0a-0000-4000-8000-000000000009").toString();
        String yaml = key + ":\n  name: stored\n" + body.indent(2);
        if (body.contains("name:")) {
            yaml = key + ":\n" + body.indent(2);
        }
        return codec.deserialize(key, CodecSamples.section(yaml, key));
    }
}
