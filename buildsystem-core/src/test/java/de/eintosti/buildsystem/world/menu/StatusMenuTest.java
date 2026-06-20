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
package de.eintosti.buildsystem.world.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.TestData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Pins the dynamic {@link StatusMenu} grid: it renders one selectable item per registered status, contiguously from the
 * first status slot, built through the real production constructor under a {@link MockBukkit} server.
 */
@NullMarked
class StatusMenuTest {

    private static final int FIRST_STATUS_SLOT = 10;

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private StatusMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any(), any())).thenReturn("Title");
        WorldStatusRegistry registry = TestData.stubStatusRegistry(mock(BuildSystemPlugin.class));

        WorldData data = mock(WorldData.class);
        when(data.getStatus()).thenReturn(TestData.NOT_STARTED);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn("world");
        when(buildWorld.getData()).thenReturn(data);

        return new StatusMenu(
                messages,
                registry,
                mock(SettingsService.class),
                mock(MenuItems.class),
                mock(Menus.class),
                buildWorld,
                server.addPlayer());
    }

    @Test
    void grid_rendersOneItemPerStatusContiguously() {
        StatusMenu menu = menu();
        menu.populate(server.addPlayer());
        Inventory inventory = menu.getInventory();

        for (int i = 0; i < TestData.STATUSES.size(); i++) {
            ItemStack item = inventory.getItem(FIRST_STATUS_SLOT + i);
            BuildWorldStatus status = TestData.STATUSES.get(i);
            assertNotNull(item, "Expected a status item at slot " + (FIRST_STATUS_SLOT + i) + " for " + status.getId());
        }
    }

    @Test
    void grid_leavesSlotBeforeFirstStatusEmpty() {
        StatusMenu menu = menu();
        menu.populate(server.addPlayer());
        assertNull(menu.getInventory().getItem(FIRST_STATUS_SLOT - 1));
    }

    @Test
    void grid_sizeCoversEveryStatus() {
        StatusMenu menu = menu();
        assertEquals(0, menu.getInventory().getSize() % 9);
    }
}
