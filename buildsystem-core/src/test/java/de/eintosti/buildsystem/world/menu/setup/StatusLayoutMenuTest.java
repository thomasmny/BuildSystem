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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;

@NullMarked
class StatusLayoutMenuTest {

    private static final int STATUS_SLOT = 10;
    private static final int DELETE_SLOT = 8;

    private ServerMock server;
    private MockedStatic<ItemBuilder> itemBuilder;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // XSkull needs authlib, which only the server provides; plain heads are enough here.
        itemBuilder = mockStatic(ItemBuilder.class, CALLS_REAL_METHODS);
        itemBuilder
                .when(() -> ItemBuilder.skull(any(Profileable.class)))
                .thenAnswer(invocation -> ItemBuilder.of(Material.PLAYER_HEAD));
    }

    @AfterEach
    void tearDown() {
        itemBuilder.close();
        MockBukkit.unmock();
    }

    @Test
    void droppingTheLastStatusOnTheBinTellsThePlayerWhy() {
        BuildWorldStatus status = mock(BuildWorldStatus.class);
        when(status.getId()).thenReturn("only");
        when(status.getSlot()).thenReturn(STATUS_SLOT);
        when(status.isShown()).thenReturn(true);
        when(status.getIcon()).thenReturn(Material.STONE);
        when(status.getStyledName()).thenReturn("Only");
        WorldStatusRegistryImpl registry = mock(WorldStatusRegistryImpl.class);
        when(registry.getAll()).thenReturn(List.of(status));
        when(registry.delete("only")).thenReturn(false);

        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("text");
        SoundlessPlayer player = SoundlessPlayer.join(server, "Admin");
        StatusLayoutMenu menu = new StatusLayoutMenu(
                messages,
                mock(MenuItems.class),
                mock(Menus.class),
                mock(TaskScheduler.class),
                mock(Prompts.class),
                registry,
                mock(NavigatorEditorService.class),
                player);
        menu.open(player);
        InventoryView view = player.getOpenInventory();

        menu.handleClick(new InventoryClickEvent(
                view, SlotType.CONTAINER, STATUS_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        int deleteRawSlot = view.getTopInventory().getSize() + 27 + DELETE_SLOT;
        menu.handleClick(new InventoryClickEvent(
                view, SlotType.QUICKBAR, deleteRawSlot, ClickType.LEFT, InventoryAction.PLACE_ALL));

        verify(registry).delete("only");
        verify(messages).sendMessage(player, "setup_delete_last");
    }

    @Test
    void deletingAnotherStatusStaysSilent() {
        BuildWorldStatus status = mock(BuildWorldStatus.class);
        when(status.getId()).thenReturn("one");
        when(status.getSlot()).thenReturn(STATUS_SLOT);
        when(status.isShown()).thenReturn(true);
        when(status.getIcon()).thenReturn(Material.STONE);
        when(status.getStyledName()).thenReturn("One");
        WorldStatusRegistryImpl registry = mock(WorldStatusRegistryImpl.class);
        when(registry.getAll()).thenReturn(List.of(status));
        when(registry.delete("one")).thenReturn(true);

        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("text");
        SoundlessPlayer player = SoundlessPlayer.join(server, "Admin");
        StatusLayoutMenu menu = new StatusLayoutMenu(
                messages,
                mock(MenuItems.class),
                mock(Menus.class),
                mock(TaskScheduler.class),
                mock(Prompts.class),
                registry,
                mock(NavigatorEditorService.class),
                player);
        menu.open(player);
        InventoryView view = player.getOpenInventory();

        menu.handleClick(new InventoryClickEvent(
                view, SlotType.CONTAINER, STATUS_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        int deleteRawSlot = view.getTopInventory().getSize() + 27 + DELETE_SLOT;
        menu.handleClick(new InventoryClickEvent(
                view, SlotType.QUICKBAR, deleteRawSlot, ClickType.LEFT, InventoryAction.PLACE_ALL));

        verify(registry).delete("one");
        verify(messages, never()).sendMessage(player, "setup_delete_last");
    }
}
