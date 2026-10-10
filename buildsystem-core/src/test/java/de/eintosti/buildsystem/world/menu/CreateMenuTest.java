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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import java.io.File;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;

@NullMarked
class CreateMenuTest {

    private static final int FIRST_TEMPLATE_SLOT = 29;

    @TempDir
    File dataFolder;

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
    void template_isCreatedFromItsFolderName_whateverTheItemIsCalled() {
        new File(dataFolder, "templates/castle").mkdirs();
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("text");
        when(messages.getString(eq("create_template"), any(), any())).thenReturn("§6Template » castle");
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        SoundlessPlayer player = SoundlessPlayer.join(server, "Alex");

        CreateMenu menu = new CreateMenu(
                messages,
                mock(MenuItems.class),
                mock(Menus.class),
                worldService,
                mock(CustomizableIcons.class),
                dataFolder,
                CreateMenu.Page.TEMPLATES,
                Visibility.EVERYONE,
                null,
                player);
        menu.open(player);
        menu.handleClick(new InventoryClickEvent(
                player.getOpenInventory(),
                SlotType.CONTAINER,
                FIRST_TEMPLATE_SLOT,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));

        verify(worldService)
                .startWorldNameInput(eq(player), eq(BuildWorldType.TEMPLATE), eq("castle"), any(), isNull());
    }
}
