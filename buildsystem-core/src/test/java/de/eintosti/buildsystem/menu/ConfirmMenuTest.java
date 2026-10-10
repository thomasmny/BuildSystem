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
package de.eintosti.buildsystem.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

@NullMarked
class ConfirmMenuTest {

    private ServerMock server;
    private SoundlessPlayer player;
    private final List<String> calls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = SoundlessPlayer.join(server, "Alex");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void layout_putsConfirmInfoAndCancelInTheMiddleRow() {
        ConfirmMenu menu = menu("perm", new ItemStack(Material.FILLED_MAP));
        menu.open(player);

        assertEquals(Material.LIME_DYE, menu.getInventory().getItem(11).getType());
        assertEquals(Material.FILLED_MAP, menu.getInventory().getItem(13).getType());
        assertEquals(Material.RED_DYE, menu.getInventory().getItem(15).getType());
        assertEquals(
                Material.LIME_STAINED_GLASS_PANE, menu.getInventory().getItem(0).getType());
        assertEquals(
                Material.RED_STAINED_GLASS_PANE, menu.getInventory().getItem(26).getType());
    }

    @Test
    void confirm_runsTheActionAndCloses() {
        player.setOp(true);
        ConfirmMenu menu = menu("perm", null);

        click(menu, 11);

        assertEquals(List.of("confirm"), calls);
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    @Test
    void confirm_withoutThePermission_doesNothing() {
        click(menu("perm", null), 11);

        assertEquals(List.of(), calls);
    }

    @Test
    void cancel_runsTheCancelAction() {
        click(menu("perm", null), 15);

        assertEquals(List.of("cancel"), calls);
    }

    private ConfirmMenu menu(String permission, @Nullable ItemStack info) {
        return new ConfirmMenu(
                new MenuContext(mock(Messages.class), mock(MenuItems.class), mock(Menus.class)),
                "Title",
                new ConfirmMenu.Choice(new ItemStack(Material.LIME_DYE), p -> calls.add("confirm")),
                permission,
                new ConfirmMenu.Choice(new ItemStack(Material.RED_DYE), p -> calls.add("cancel")),
                info);
    }

    private void click(ConfirmMenu menu, int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.openInventory(menu.getInventory()),
                SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL);
        menu.handleClick(event);
    }
}
