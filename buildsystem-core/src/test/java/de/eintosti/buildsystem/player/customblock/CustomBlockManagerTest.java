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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.util.TaskScheduler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;

@NullMarked
class CustomBlockManagerTest {

    private PlayerMock player;
    private TaskScheduler scheduler;
    private CustomBlockManager manager;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        player = server.addPlayer();
        scheduler = mock(TaskScheduler.class);
        manager = new CustomBlockManager(mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS), scheduler);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void placingACustomBlock_replacesTheHeadOnTheNextTick() {
        BlockPlaceEvent event = placeHead();

        manager.onCustomBlockPlace(event);
        runNextTick();

        assertEquals(Material.OAK_WOOD, event.getBlockPlaced().getType());
    }

    @Test
    void placingACustomBlock_thatALaterListenerCancels_leavesTheBlockAlone() {
        BlockPlaceEvent event = placeHead();

        manager.onCustomBlockPlace(event);
        // The protection check runs at a later priority than the custom blocks.
        event.setCancelled(true);
        runNextTick();

        assertEquals(Material.PLAYER_HEAD, event.getBlockPlaced().getType());
    }

    private BlockPlaceEvent placeHead() {
        Block block = player.getWorld().getBlockAt(0, 64, 0);
        block.setType(Material.PLAYER_HEAD);
        ItemStack head = CustomBlock.FULL_OAK_BARCH.storeCustomBlock(new ItemStack(Material.PLAYER_HEAD));
        return new BlockPlaceEvent(
                block, block.getState(), block.getRelative(0, -1, 0), head, player, true, EquipmentSlot.HAND);
    }

    private void runNextTick() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).run(task.capture());
        task.getValue().run();
    }
}
