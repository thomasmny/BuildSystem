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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.WorldDisplay;
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.menu.CreateMenu.Page;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;

/**
 * The bottom bar of the world navigator menus: sort, filter, the create buttons and going back.
 */
@NullMarked
class DisplayablesMenuTest {

    private ServerMock server;
    private MockedStatic<ItemBuilder> itemBuilder;
    private SoundlessPlayer player;
    private Menus menus;
    private WorldDisplay worldDisplay;
    private NavigatorCategory category;
    private DisplayablesContext context;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        itemBuilder = mockStatic(ItemBuilder.class, CALLS_REAL_METHODS);
        itemBuilder
                .when(() -> ItemBuilder.skull(any(Profileable.class)))
                .thenAnswer(invocation -> ItemBuilder.of(Material.PLAYER_HEAD));
        player = SoundlessPlayer.join(server, "Alex");
        player.setOp(true);

        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("text");
        when(messages.getString(anyString(), any(), any())).thenReturn("text");

        worldDisplay = mock(WorldDisplay.class);
        when(worldDisplay.getWorldSort()).thenReturn(WorldSort.NAME_A_TO_Z);
        WorldFilter filter = mock(WorldFilter.class);
        when(filter.getMode()).thenReturn(WorldFilter.Mode.NONE);
        when(filter.getText()).thenReturn("");
        when(filter.apply()).thenReturn(world -> true);
        when(worldDisplay.getWorldFilter()).thenReturn(filter);
        Settings settings = mock(Settings.class);
        when(settings.getWorldDisplay()).thenReturn(worldDisplay);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings(any())).thenReturn(settings);

        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getFolderStorage()).thenReturn(mock(FolderStorageImpl.class));
        when(worldService.getWorldStorage()).thenReturn(mock(WorldStorageImpl.class));
        PlayerServiceImpl playerService = mock(PlayerServiceImpl.class);
        when(playerService.canCreateWorld(any(), any())).thenReturn(true);
        menus = mock(Menus.class);

        category = mock(NavigatorCategory.class);
        when(category.getId()).thenReturn("public");
        when(category.getDisplayName()).thenReturn("Public");
        when(category.getVisibilities()).thenReturn(Set.of(Visibility.EVERYONE));
        when(category.getPrimaryVisibility()).thenReturn(Visibility.EVERYONE);
        when(category.getStatusIds()).thenReturn(List.of(TestData.NOT_STARTED.getId()));

        context = new DisplayablesContext(
                messages,
                playerService,
                settingsService,
                worldService,
                mock(MenuItems.class),
                mock(Prompts.class),
                mock(NavigatorService.class),
                menus);
    }

    @AfterEach
    void tearDown() {
        itemBuilder.close();
        MockBukkit.unmock();
    }

    @Test
    void createWorld_opensTheCreateMenu() {
        click(categoryMenu(), 48);

        verify(menus).openCreate(Page.PREDEFINED, Visibility.EVERYONE, null, player);
    }

    @Test
    void createWorld_insideAFolder_createsInThatFolder() {
        Folder folder = mock(Folder.class);
        when(folder.getName()).thenReturn("Folder");
        FolderContentMenu menu = new FolderContentMenu(context, player, category, folder, categoryMenu());

        click(menu, 48);

        verify(menus).openCreate(Page.PREDEFINED, Visibility.EVERYONE, folder, player);
    }

    @Test
    void sort_cyclesTheSort() {
        click(categoryMenu(), 45);

        verify(worldDisplay).setWorldSort(WorldSort.NAME_A_TO_Z.getNext());
    }

    @Test
    void bottomBarFiller_goesBack() {
        CategoryWorldsMenu menu = categoryMenu();
        menu.open(player);
        menu.getInventory().setItem(51, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));

        click(menu, 51);

        verify(menus).openNavigator(player);
        verify(menus, never()).openCreate(any(), any(), any(), any());
    }

    private CategoryWorldsMenu categoryMenu() {
        return new CategoryWorldsMenu(context, TestData.statusRegistry(), player, category);
    }

    private void click(DisplayablesMenu menu, int slot) {
        if (!menu.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            menu.open(player);
        }
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(), SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        menu.handleClick(event);
    }
}
