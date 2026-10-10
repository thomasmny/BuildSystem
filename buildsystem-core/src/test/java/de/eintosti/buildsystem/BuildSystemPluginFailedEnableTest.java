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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import de.eintosti.buildsystem.util.TaskScheduler;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedConstruction;

class BuildSystemPluginFailedEnableTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void enableFailingBeforeTheServicesExist_disablesCleanlyAndStopsTheScheduler() {
        try (MockedConstruction<TaskScheduler> schedulers = mockConstruction(TaskScheduler.class);
                MockedConstruction<Services> ignored = mockConstruction(Services.class, (services, context) -> {
                    throw new IllegalStateException("services failed");
                })) {
            // A real server catches what onEnable throws and disables the plugin; MockBukkit only rethrows it.
            assertThrows(RuntimeException.class, () -> MockBukkit.load(BuildSystemPlugin.class));
            Plugin plugin = server.getPluginManager().getPlugin("BuildSystem");
            assertDoesNotThrow(() -> server.getPluginManager().disablePlugin(plugin));

            verify(schedulers.constructed().getFirst()).shutdown();
        }
    }
}
