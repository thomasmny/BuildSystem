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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.util.TaskScheduler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CustomBlockManager implements Listener {

    private final TaskScheduler scheduler;

    public CustomBlockManager(BuildSystemPlugin plugin, TaskScheduler scheduler) {
        this.scheduler = scheduler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCustomBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        ItemStack itemStack = event.getItemInHand();
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            return;
        }

        CustomBlock customBlock = CustomBlock.of(itemStack);
        if (customBlock == null) {
            return;
        }

        setBlock(event, customBlock);
    }

    /**
     * Replaces the placed player head with the custom block on the next tick. The block data must be applied a tick
     * later, after Bukkit finishes placing the head.
     *
     * @param event The placement that dropped the custom-block head
     * @param customBlock The custom block to materialize
     */
    public void setBlock(BlockPlaceEvent event, CustomBlock customBlock) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        scheduler.run(() -> {
            // This listener runs before the world protection, which can still cancel the placement after it. A tick
            // later every listener has had its say.
            if (!event.isCancelled()) {
                customBlock.place(block, player);
            }
        });
    }

    /**
     * Makes an {@link CustomBlock#INVISIBLE_ITEM_FRAME} invisible on placement.
     *
     * @param event The item-frame placement
     */
    @EventHandler
    public void onInvisibleItemFramePlacement(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame)) {
            return;
        }

        if (CustomBlock.of(event.getItemStack()) != CustomBlock.INVISIBLE_ITEM_FRAME) {
            return;
        }

        itemFrame.setVisible(false);
    }
}
