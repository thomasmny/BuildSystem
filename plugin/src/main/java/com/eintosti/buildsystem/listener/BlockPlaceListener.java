/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public class BlockPlaceListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public BlockPlaceListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCustomBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        boolean isBuildWorld = buildWorld != null;

        ItemStack itemStack = player.getItemInHand();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (xMaterial != XMaterial.PLAYER_HEAD) return;

        boolean hadToDisablePhysics = false;
        if (isBuildWorld && !buildWorld.isPhysics()) {
            hadToDisablePhysics = true;
            buildWorld.setPhysics(true);
        }

        plugin.getCustomBlocks().setBlock(event,
                plugin.getString("blocks_full_oak_barch"),
                plugin.getString("blocks_full_spruce_barch"),
                plugin.getString("blocks_full_birch_barch"),
                plugin.getString("blocks_full_jungle_barch"),
                plugin.getString("blocks_full_acacia_barch"),
                plugin.getString("blocks_full_dark_oak_barch"),
                plugin.getString("blocks_red_mushroom"),
                plugin.getString("blocks_brown_mushroom"),
                plugin.getString("blocks_full_mushroom_stem"),
                plugin.getString("blocks_mushroom_stem"),
                plugin.getString("blocks_mushroom_block"),
                plugin.getString("blocks_smooth_stone"),
                plugin.getString("blocks_double_stone_slab"),
                plugin.getString("blocks_smooth_sandstone"),
                plugin.getString("blocks_smooth_red_sandstone"),
                plugin.getString("blocks_powered_redstone_lamp"),
                plugin.getString("blocks_burning_furnace"),
                plugin.getString("blocks_command_block"),
                plugin.getString("blocks_barrier"),
                plugin.getString("blocks_mob_spawner"),
                plugin.getString("blocks_nether_portal"),
                plugin.getString("blocks_end_portal"),
                plugin.getString("blocks_dragon_egg")
        );

        if (isBuildWorld && hadToDisablePhysics) {
            buildWorld.setPhysics(false);
        }
    }
}
