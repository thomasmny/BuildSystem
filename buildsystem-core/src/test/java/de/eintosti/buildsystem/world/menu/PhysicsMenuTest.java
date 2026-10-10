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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Pins which world-data key each {@link PhysicsMenu} slot flips, by clicking it. Built through the real production
 * constructor under a {@link MockBukkit} server.
 */
@NullMarked
class PhysicsMenuTest {

    private static final int SIZE = 45;

    private Messages messages;
    private WorldDataImpl data;
    private BuildWorld buildWorld;
    private SoundlessPlayer player;

    @BeforeEach
    void setUp() {
        player = SoundlessPlayer.join(MockBukkit.mock(), "Alex");
        messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        data = WorldDataSchema.create("world", TestData.NOT_STARTED);
        buildWorld = mock(BuildWorld.class);
        when(buildWorld.getData()).thenReturn(data);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    static Stream<Arguments> slots() {
        return Stream.of(
                Arguments.of(4, WorldDataKey.PHYSICS),
                Arguments.of(12, PhysicsCategory.BLOCK_UPDATES.key()),
                Arguments.of(13, PhysicsCategory.CONNECTIONS.key()),
                Arguments.of(14, PhysicsCategory.FALLING_BLOCKS.key()),
                Arguments.of(21, PhysicsCategory.FLUID_FLOW.key()),
                Arguments.of(22, PhysicsCategory.LEAF_DECAY.key()),
                Arguments.of(23, PhysicsCategory.GROWTH.key()),
                Arguments.of(30, PhysicsCategory.SPREADING.key()),
                Arguments.of(31, PhysicsCategory.BLOCK_FORMING.key()),
                Arguments.of(32, PhysicsCategory.BLOCK_FADING.key()));
    }

    @ParameterizedTest(name = "slot {0} flips {1}")
    @MethodSource("slots")
    void slot_flipsItsKey_forAPlayerWhoMayEditPhysics(int slot, WorldDataKey<Boolean> key) {
        boolean before = data.get(key);

        click(slot);
        verify(messages).sendPermissionError(player);
        assertEquals(before, data.get(key));

        player.addAttachment(MockBukkit.createMockPlugin(), Permissions.EDIT_PHYSICS, true);
        click(slot);
        assertEquals(!before, data.get(key));
        assertEquals(
                before ? Sound.BLOCK_COPPER_BULB_TURN_OFF : Sound.BLOCK_COPPER_BULB_TURN_ON,
                player.sounds().getLast());
    }

    @Test
    void clickingEverySlot_flipsTheMasterAndEachCategoryOnce() {
        player.setOp(true);
        Set<WorldDataKey<Boolean>> keys = new HashSet<>();
        keys.add(WorldDataKey.PHYSICS);
        Arrays.stream(PhysicsCategory.values()).map(PhysicsCategory::key).forEach(keys::add);
        Set<WorldDataKey<Boolean>> flipped = new HashSet<>();

        for (int slot = 0; slot < SIZE; slot++) {
            Map<WorldDataKey<Boolean>, Boolean> before = new HashMap<>();
            keys.forEach(key -> before.put(key, data.get(key)));
            click(slot);
            for (WorldDataKey<Boolean> key : keys) {
                if (!data.get(key).equals(before.get(key))) {
                    assertTrue(flipped.add(key), key + " is flipped by two slots");
                    data.set(key, before.get(key));
                }
            }
        }

        assertEquals(keys, flipped);
    }

    private void click(int slot) {
        PhysicsMenu menu = new PhysicsMenu(
                new MenuContext(messages, mock(MenuItems.class), mock(Menus.class)), buildWorld, player);
        menu.handleClick(new InventoryClickEvent(
                player.openInventory(menu.getInventory()),
                SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));
    }
}
