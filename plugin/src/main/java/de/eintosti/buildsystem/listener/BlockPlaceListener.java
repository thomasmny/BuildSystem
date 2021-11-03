/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
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
    @SuppressWarnings("deprecation")
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);

        if (buildWorld == null) {
            return;
        }

        if (disableArchivedWorlds(buildWorld, player, event)) return;
        if (checkWorldSettings(buildWorld, player, event)) return;
        if (checkBuilders(buildWorld, player, event)) return;

        setStatus(buildWorld, player);

        ItemStack itemStack = player.getItemInHand();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);

        if (xMaterial == XMaterial.PLAYER_HEAD) {
            boolean hadToDisablePhysics = false;
            if (!buildWorld.isPhysics()) {
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

            if (hadToDisablePhysics) {
                buildWorld.setPhysics(false);
            }
        }
    }

    private boolean disableArchivedWorlds(BuildWorld buildWorld, Player player, BlockPlaceEvent event) {
        if (!plugin.canBypass(player) && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean checkWorldSettings(BuildWorld buildWorld, Player player, BlockPlaceEvent event) {
        if (!plugin.canBypass(player) && buildWorld.isBlockPlacement()) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean checkBuilders(BuildWorld buildWorld, Player player, BlockPlaceEvent event) {
        if (plugin.canBypass(player)) return false;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return false;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }

    private void setStatus(BuildWorld buildWorld, Player player) {
        if (buildWorld.getStatus() == WorldStatus.NOT_STARTED) {
            buildWorld.setStatus(WorldStatus.IN_PROGRESS);
            plugin.forceUpdateSidebar(player);
        }
    }
}
