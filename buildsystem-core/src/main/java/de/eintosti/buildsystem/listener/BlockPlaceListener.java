/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.version.customblocks.CustomBlock;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class BlockPlaceListener implements Listener {

    private final BuildSystem plugin;
    private final WorldManager worldManager;

    private final Map<String, String> blockLookup;

    public BlockPlaceListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        this.blockLookup = initBlockLookup();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Map<String, String> initBlockLookup() {
        Map<String, String> lookup = new HashMap<>();
        for (CustomBlock customBlock : CustomBlock.values()) {
            lookup.put(Messages.getString(customBlock.getKey()), customBlock.getKey());
        }
        return lookup;
    }

    @EventHandler
    public void onCustomBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        boolean isBuildWorld = buildWorld != null;

        ItemStack itemStack = event.getItemInHand();
        ItemMeta itemMeta = itemStack.getItemMeta();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (xMaterial != XMaterial.PLAYER_HEAD) {
            return;
        }

        boolean hadToDisablePhysics = false;
        if (isBuildWorld && !buildWorld.getData().physics().get()) {
            hadToDisablePhysics = true;
            buildWorld.getData().physics().set(true);
        }

        plugin.getCustomBlocks().setBlock(event, blockLookup.get(itemMeta.getDisplayName()));

        if (isBuildWorld && hadToDisablePhysics) {
            buildWorld.getData().physics().set(false);
        }
    }
}