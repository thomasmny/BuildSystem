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
package de.eintosti.buildsystem.world.menu.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

@NullMarked
class CategoryStatusesMenuTest {

    private static final int FIRST_STATUS_SLOT = 9;

    private SoundlessPlayer player;

    @BeforeEach
    void setUp() {
        player = SoundlessPlayer.join(MockBukkit.mock(), "Admin");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void togglingMembership_playsTheOnOrOffSoundForTheStateItEndsIn() {
        BuildWorldStatus status = mock(BuildWorldStatus.class);
        when(status.getId()).thenReturn("one");
        when(status.getIcon()).thenReturn(Material.STONE);
        when(status.getStyledName()).thenReturn("one");
        WorldStatusRegistryImpl statuses = mock(WorldStatusRegistryImpl.class);
        when(statuses.getAll()).thenReturn(List.of(status));
        NavigatorCategoryImpl category = NavigatorCategoryImpl.builder("category")
                .statusIds(new ArrayList<>())
                .build();

        CategoryStatusesMenu menu = menu(statuses, category);
        click(menu);
        click(menu);

        assertEquals(List.of(Sound.BLOCK_COPPER_BULB_TURN_ON, Sound.BLOCK_COPPER_BULB_TURN_OFF), player.sounds());
    }

    private CategoryStatusesMenu menu(WorldStatusRegistryImpl statuses, NavigatorCategoryImpl category) {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("text");
        CategoryStatusesMenu menu = new CategoryStatusesMenu(
                new MenuContext(messages, mock(MenuItems.class, RETURNS_DEEP_STUBS), mock(Menus.class)),
                mock(NavigatorCategoryRegistryImpl.class),
                statuses,
                player,
                category);
        menu.open(player);
        return menu;
    }

    private void click(CategoryStatusesMenu menu) {
        menu.handleClick(new InventoryClickEvent(
                player.getOpenInventory(),
                SlotType.CONTAINER,
                FIRST_STATUS_SLOT,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));
    }
}
