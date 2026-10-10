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
package de.eintosti.buildsystem.player.customblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;

/**
 * Pins the block each {@link CustomBlockMenu} slot gives, by clicking it. Built through the real production constructor
 * under a {@link MockBukkit} server.
 */
@NullMarked
class CustomBlockMenuTest {

    private Messages messages;
    private SoundlessPlayer player;
    private MockedStatic<ItemBuilder> itemBuilder;

    @BeforeEach
    void setUp() {
        player = SoundlessPlayer.join(MockBukkit.mock(), "Alex");
        messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
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

    @ParameterizedTest(name = "slot {0} gives {1}")
    @CsvSource({
        "1, FULL_OAK_BARCH, PLAYER_HEAD",
        "14, MUSHROOM_BLOCK, PLAYER_HEAD",
        "19, SMOOTH_STONE, PLAYER_HEAD",
        "31, COMMAND_BLOCK, PLAYER_HEAD",
        "32, BARRIER, BARRIER",
        "33, INVISIBLE_ITEM_FRAME, ITEM_FRAME",
        "40, DRAGON_EGG, PLAYER_HEAD",
        "41, DEBUG_STICK, DEBUG_STICK"
    })
    void slot_givesItsBlock(int slot, CustomBlock block, Material material) {
        click(slot);

        ItemStack given = player.getInventory().getItem(0);
        assertEquals(block, CustomBlock.of(given));
        assertEquals(material, given.getType());
    }

    @Test
    void everySlot_givesTwentySixDifferentBlocks_andGlassOrEmptySlotsNothing() {
        for (int slot = 0; slot < 45; slot++) {
            click(slot);
        }

        List<CustomBlock> given = Arrays.stream(player.getInventory().getContents())
                .map(CustomBlock::of)
                .filter(Objects::nonNull)
                .toList();
        assertEquals(26, given.size());
        assertEquals(26, given.stream().distinct().count());
    }

    private void click(int slot) {
        CustomBlockMenu menu = new CustomBlockMenu(messages, mock(MenuItems.class), player);
        menu.handleClick(new InventoryClickEvent(
                player.openInventory(menu.getInventory()),
                SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));
    }
}
