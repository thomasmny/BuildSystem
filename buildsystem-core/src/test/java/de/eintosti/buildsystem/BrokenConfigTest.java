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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedConstruction;

/**
 * A config.yml with broken syntax, which Bukkit would load as empty and a save would then replace with the defaults.
 */
@NullMarked
class BrokenConfigTest {

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;
    private Path config;
    private String broken;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        plugin = MockBukkit.load(BuildSystemPlugin.class);
        config = new File(plugin.getDataFolder(), "config.yml").toPath();
        broken = Files.readString(config).replace("  type: sqlite", "  type: postgresql") + "broken: [unclosed\n";
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void configReload_keepsTheFileAndTheSettingsInUse() throws Exception {
        Files.writeString(config, broken);

        assertFalse(plugin.reloadConfigData(true));

        assertEquals(broken, Files.readString(config));
        assertEquals("sqlite", plugin.getConfig().getString("storage.type"));
    }

    @Test
    void disable_keepsTheFile() throws Exception {
        Files.writeString(config, broken);

        server.getPluginManager().disablePlugin(plugin);

        assertEquals(broken, Files.readString(config));
    }

    @Test
    void load_refusesToStart_andKeepsTheFile() throws Exception {
        server.getPluginManager().disablePlugin(plugin);
        Files.writeString(config, broken);

        assertThrows(IllegalStateException.class, plugin::onLoad);

        assertEquals(broken, Files.readString(config));
    }
}
