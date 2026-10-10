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
package de.eintosti.buildsystem.config.migration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class ConfigMigrationManagerTest {

    @TempDir
    File dataFolder;

    @Test
    void freshInstall_runsNoMigrationAndWritesNoBackup() {
        YamlConfiguration config = new YamlConfiguration();
        BuildSystemPlugin plugin = plugin(config);

        new ConfigMigrationManager(plugin).migrate();

        verify(plugin, never()).getConfig();
        verify(plugin, never()).saveConfig();
        assertFalse(config.contains("version"));
        assertArrayEquals(new String[0], dataFolder.list());
    }

    @Test
    void configWithoutVersion_migratesToTheLatestVersion() throws IOException {
        File configFile = new File(dataFolder, "config.yml");
        Files.writeString(configFile.toPath(), """
                settings:
                  update-checker: true
                """);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        new ConfigMigrationManager(plugin(config)).migrate();

        assertEquals(ConfigMigrationManager.LATEST_VERSION, config.getInt("version"));
        assertTrue(new File(dataFolder, "config.yml.v1.bak").exists());
    }

    private BuildSystemPlugin plugin(YamlConfiguration config) {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        return plugin;
    }
}
