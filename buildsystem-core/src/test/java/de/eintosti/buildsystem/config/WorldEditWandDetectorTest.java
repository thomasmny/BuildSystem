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
package de.eintosti.buildsystem.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptomorin.xseries.XMaterial;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class WorldEditWandDetectorTest {

    @TempDir
    Path pluginsDir;

    private void writeConfig(String pluginDir, String fileName, String yaml) throws Exception {
        Path dir = Files.createDirectories(pluginsDir.resolve(pluginDir));
        Files.writeString(dir.resolve(fileName), yaml);
    }

    @Test
    void detect_nullDir_returnsDefault() {
        assertEquals(WorldEditWandDetector.DEFAULT_WAND, new WorldEditWandDetector(null).detect());
    }

    @Test
    void detect_noWorldEditInstalled_returnsDefault() {
        assertEquals(WorldEditWandDetector.DEFAULT_WAND, new WorldEditWandDetector(pluginsDir.toFile()).detect());
    }

    @Test
    void detect_worldEditConfig_readsAndStripsNamespace() throws Exception {
        writeConfig("WorldEdit", "config.yml", "wand-item: minecraft:blaze_rod");
        assertEquals(XMaterial.BLAZE_ROD, new WorldEditWandDetector(pluginsDir.toFile()).detect());
    }

    @Test
    void detect_fastAsyncWorldEditTakesPrecedence() throws Exception {
        writeConfig("WorldEdit", "config.yml", "wand-item: minecraft:blaze_rod");
        writeConfig("FastAsyncWorldEdit", "worldedit-config.yml", "wand-item: minecraft:stick");
        assertEquals(XMaterial.STICK, new WorldEditWandDetector(pluginsDir.toFile()).detect());
    }

    @Test
    void detect_missingWandKey_returnsDefault() throws Exception {
        writeConfig("WorldEdit", "config.yml", "some-other-key: 5");
        assertEquals(WorldEditWandDetector.DEFAULT_WAND, new WorldEditWandDetector(pluginsDir.toFile()).detect());
    }
}
