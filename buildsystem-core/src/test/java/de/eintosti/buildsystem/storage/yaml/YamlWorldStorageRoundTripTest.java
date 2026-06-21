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
package de.eintosti.buildsystem.storage.yaml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round-trip and parse-error tests for {@link YamlWorldStorage}. Establishes the contract for the on-disk
 * {@code worlds.yml} format: a world serialized and saved must deserialize back with all fields intact; malformed enum
 * data falls back to a safe default; and an entry that cannot be parsed at all is skipped so the remaining worlds still
 * load (one bad row never aborts the whole load).
 */
class YamlWorldStorageRoundTripTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        // Deep stubs return false for world().unload().enabled(), so BuildWorldImpl construction
        // never touches the Bukkit scheduler (manageUnload short-circuits). The unload time string
        // is parsed unconditionally in the WorldUnloader constructor, so it must be a real value.
        when(plugin.getConfigService().current().world().unload().timeUntilUnload())
                .thenReturn("06:00:00");
        TestData.stubStatusRegistry(plugin);
    }

    private YamlWorldStorage newStorage() {
        return new YamlWorldStorage(plugin);
    }

    private BuildWorldImpl sampleWorld(UUID uuid, String name) {
        Builder creator = Builder.of(UUID.randomUUID(), "Creator");
        Builder extraBuilder = Builder.of(UUID.randomUUID(), "Helper");
        WorldDataImpl data = new WorldDataBuilder(name)
                .withStatus(TestData.FINISHED)
                .withDifficulty(Difficulty.NORMAL)
                .withMaterial(XMaterial.GRASS_BLOCK)
                .withPermission("buildsystem.test")
                .withProject("MyProject")
                .withVisibility(Visibility.ADDED_PLAYERS)
                .withBlockBreaking(true)
                .withExplosions(true)
                .withTimeSinceBackup(42)
                .withPermissionOverrideEnabled(() -> false)
                .withProjectOverrideEnabled(() -> false)
                .build();
        return new BuildWorldImpl(
                plugin,
                uuid,
                name,
                BuildWorldType.NORMAL,
                data,
                creator,
                List.of(creator, extraBuilder),
                1_700_000_000_000L,
                null,
                null);
    }

    @Test
    void roundTrip_preservesCoreFields() {
        UUID uuid = UUID.randomUUID();
        newStorage().save(sampleWorld(uuid, "TestWorld")).join();

        Collection<BuildWorld> loaded = newStorage().load().join();
        assertEquals(1, loaded.size());
        BuildWorld world = loaded.iterator().next();
        assertEquals(uuid, world.getUniqueId());
        assertEquals("TestWorld", world.getName());
        assertEquals(BuildWorldType.NORMAL, world.getType());
        assertEquals(1_700_000_000_000L, world.getCreation());
    }

    @Test
    void roundTrip_preservesWorldData() {
        newStorage().save(sampleWorld(UUID.randomUUID(), "DataWorld")).join();

        BuildWorld world = newStorage().load().join().iterator().next();
        assertEquals(TestData.FINISHED, world.getData().getStatus());
        assertEquals("MyProject", world.getData().getProject());
        assertEquals("buildsystem.test", world.getData().getPermission());
        assertEquals(Difficulty.NORMAL, world.getData().getDifficulty());
        assertTrue(world.getData().getVisibility().isPrivate());
        assertTrue(world.getData().isBlockBreaking());
        assertEquals(42, world.getData().getTimeSinceBackup());
    }

    @Test
    void roundTrip_preservesBuilders() {
        newStorage().save(sampleWorld(UUID.randomUUID(), "BuilderWorld")).join();

        BuildWorld world = newStorage().load().join().iterator().next();
        assertEquals("Creator", world.getBuilders().getCreator().getName());
        assertEquals(2, world.getBuilders().getAllBuilders().size());
    }

    @Test
    void roundTrip_preservesCustomSpawn() {
        BuildWorldImpl world = sampleWorld(UUID.randomUUID(), "SpawnWorld");
        world.getData().setCustomSpawn("1.0;64.0;2.0;90.0;0.0");
        newStorage().save(world).join();

        BuildWorld loaded = newStorage().load().join().iterator().next();
        assertEquals("1.0;64.0;2.0;90.0;0.0", loaded.getData().getCustomSpawn());
    }

    @Test
    void load_legacyTopLevelSpawn_isReadAsFallback() throws Exception {
        // Pre-property-map files stored the custom spawn at the top-level "spawn" key, not under "data".
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.Legacy.uuid", UUID.randomUUID().toString());
        yaml.set("worlds.Legacy.type", "NORMAL");
        yaml.set("worlds.Legacy.date", 1L);
        yaml.set("worlds.Legacy.data.status", "FINISHED");
        yaml.set("worlds.Legacy.spawn", "5.0;70.0;5.0;0.0;0.0");
        yaml.save(new File(dataFolder, "worlds.yml"));

        BuildWorld loaded = newStorage().load().join().iterator().next();
        assertEquals("5.0;70.0;5.0;0.0;0.0", loaded.getData().getCustomSpawn());
    }

    @Test
    void load_emptyFile_returnsEmptyCollection() {
        assertTrue(newStorage().load().join().isEmpty());
    }

    @Test
    void load_invalidTypeEnum_defaultsToUnknown() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.Bad.uuid", UUID.randomUUID().toString());
        yaml.set("worlds.Bad.type", "NOT_A_REAL_TYPE");
        yaml.set("worlds.Bad.date", 1L);
        yaml.set("worlds.Bad.data.status", "FINISHED");
        yaml.save(new File(dataFolder, "worlds.yml"));

        Collection<BuildWorld> loaded = newStorage().load().join();
        assertEquals(1, loaded.size());
        assertEquals(BuildWorldType.UNKNOWN, loaded.iterator().next().getType());
    }

    @Test
    void load_missingStatus_defaultsToNotStarted() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.Bad.uuid", UUID.randomUUID().toString());
        yaml.set("worlds.Bad.type", "NORMAL");
        yaml.set("worlds.Bad.date", 1L);
        // No data.status key — must fall back rather than throw.
        yaml.save(new File(dataFolder, "worlds.yml"));

        Collection<BuildWorld> loaded = newStorage().load().join();
        assertEquals(1, loaded.size());
        assertEquals(TestData.NOT_STARTED, loaded.iterator().next().getData().getStatus());
    }

    @Test
    void load_unparseableEntry_isSkipped_othersStillLoad() throws Exception {
        // Persist a good world, then inject a sibling entry whose UUID cannot be parsed.
        newStorage().save(sampleWorld(UUID.randomUUID(), "Good")).join();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File(dataFolder, "worlds.yml"));
        yaml.set("worlds.Bad.uuid", "not-a-uuid");
        yaml.set("worlds.Bad.type", "NORMAL");
        yaml.set("worlds.Bad.date", 1L);
        yaml.set("worlds.Bad.data.status", "FINISHED");
        yaml.save(new File(dataFolder, "worlds.yml"));

        Collection<BuildWorld> loaded = newStorage().load().join();
        assertEquals(1, loaded.size());
        assertEquals("Good", loaded.iterator().next().getName());
    }
}
