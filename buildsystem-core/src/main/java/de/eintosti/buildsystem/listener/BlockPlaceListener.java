/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.version.customblocks.CustomBlock;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldService;
import de.eintosti.buildsystem.world.storage.WorldStorage;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockPlaceListener implements Listener {

    private final BuildSystem plugin;
    private final WorldStorage worldStorage;

    private final Map<String, String> blockLookup;

    public BlockPlaceListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.blockLookup = initBlockLookup();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Map<String, String> initBlockLookup() {
        Map<String, String> lookup = new HashMap<>();
        for (CustomBlock customBlock : CustomBlock.values()) {
            lookup.put(Messages.getString(customBlock.getKey(), null), customBlock.getKey());
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

        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
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

        String customBlockKey = blockLookup.get(itemMeta.getDisplayName());
        if (customBlockKey != null) {
            plugin.getCustomBlocks().setBlock(event, customBlockKey);
        }

        if (isBuildWorld && hadToDisablePhysics) {
            buildWorld.getData().physics().set(false);
        }
    }
}