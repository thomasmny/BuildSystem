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
package de.eintosti.buildsystem.world.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.world.creation.GenerationDataStore.WorldGenerationData;
import de.eintosti.buildsystem.world.creation.GenerationDataStore.WorldGenerationData.CustomGeneratorData;
import de.eintosti.buildsystem.world.creation.GenerationDataStore.WorldGenerationData.PredefinedGeneratorData;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

@NullMarked
class GenerationDataStoreTest {

    @TempDir
    File tempDir;

    private GenerationDataStore store;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        store = new GenerationDataStore(Logger.getLogger("test"));
        // load() resolves the world folder via FileUtils.worldFolder. With no main world, that falls back to the
        // world container, so point the container at tempDir to keep the flat per-world layout these tests use.
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(tempDir);
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
        bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void loadDefaultsToNormalWhenFileAbsent() {
        WorldGenerationData result = store.load("no-such-world");
        assertInstanceOf(PredefinedGeneratorData.class, result);
        assertEquals(BuildWorldType.NORMAL, ((PredefinedGeneratorData) result).type());
    }

    @Test
    void loadPredefinedType() throws Exception {
        String worldName = "flat-world";
        Path worldDir = tempDir.toPath().resolve(worldName);
        Files.createDirectories(worldDir);
        Files.writeString(worldDir.resolve(".buildsystem-generator-data.txt"), "FLAT");

        WorldGenerationData result = store.load(worldName);
        assertInstanceOf(PredefinedGeneratorData.class, result);
        assertEquals(BuildWorldType.FLAT, ((PredefinedGeneratorData) result).type());
    }

    @Test
    void loadCustomGeneratorType() throws Exception {
        String worldName = "custom-world";
        Path worldDir = tempDir.toPath().resolve(worldName);
        Files.createDirectories(worldDir);
        Files.writeString(worldDir.resolve(".buildsystem-generator-data.txt"), "GENERATOR:MyPlugin:myGenerator");

        WorldGenerationData result = store.load(worldName);
        assertInstanceOf(CustomGeneratorData.class, result);
        CustomGeneratorData customData = (CustomGeneratorData) result;
        assertEquals("MyPlugin", customData.pluginName());
        assertEquals("myGenerator", customData.chunkGeneratorName());
    }

    @Test
    void loadInvalidContentDefaultsToNormal() throws Exception {
        String worldName = "bad-world";
        Path worldDir = tempDir.toPath().resolve(worldName);
        Files.createDirectories(worldDir);
        Files.writeString(worldDir.resolve(".buildsystem-generator-data.txt"), "NOT_A_VALID_TYPE");

        WorldGenerationData result = store.load(worldName);
        assertInstanceOf(PredefinedGeneratorData.class, result);
        assertEquals(BuildWorldType.NORMAL, ((PredefinedGeneratorData) result).type());
    }

    @Test
    void saveWritesFileOnFirstCall() throws Exception {
        String worldName = "save-world";
        File worldDir = new File(tempDir, worldName);
        worldDir.mkdirs();

        World world = mock(World.class);
        when(world.getWorldFolder()).thenReturn(worldDir);
        when(world.getName()).thenReturn(worldName);

        store.save(world, BuildWorldType.VOID, null);

        Path dataFile = worldDir.toPath().resolve(".buildsystem-generator-data.txt");
        assertEquals("VOID", Files.readString(dataFile).trim());
    }

    @Test
    void saveDoesNotOverwriteExistingFile() throws Exception {
        String worldName = "no-overwrite-world";
        File worldDir = new File(tempDir, worldName);
        worldDir.mkdirs();
        Path dataFile = worldDir.toPath().resolve(".buildsystem-generator-data.txt");
        Files.writeString(dataFile, "FLAT");

        World world = mock(World.class);
        when(world.getWorldFolder()).thenReturn(worldDir);
        when(world.getName()).thenReturn(worldName);

        store.save(world, BuildWorldType.VOID, null);

        assertEquals("FLAT", Files.readString(dataFile).trim());
    }

    @Test
    void saveAndLoadRoundTrip() throws Exception {
        String worldName = "round-trip-world";
        File worldDir = new File(tempDir, worldName);
        worldDir.mkdirs();

        World world = mock(World.class);
        when(world.getWorldFolder()).thenReturn(worldDir);
        when(world.getName()).thenReturn(worldName);

        store.save(world, BuildWorldType.NETHER, null);
        WorldGenerationData loaded = store.load(worldName);

        assertInstanceOf(PredefinedGeneratorData.class, loaded);
        assertEquals(BuildWorldType.NETHER, ((PredefinedGeneratorData) loaded).type());
    }
}
