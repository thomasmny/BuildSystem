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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
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

    private final BuildSystemPlugin plugin;
    private final Supplier<WorldServiceImpl> worldService;
    private final CustomBlockPlacer placer = new CustomBlockPlacer();

    public CustomBlockManager(BuildSystemPlugin plugin, Supplier<WorldServiceImpl> worldService) {
        this.plugin = plugin;
        this.worldService = worldService;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCustomBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        BuildWorld buildWorld = worldService.get().getWorldStorage().getBuildWorld(player.getWorld());
        boolean isBuildWorld = buildWorld != null;

        ItemStack itemStack = event.getItemInHand();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (xMaterial != XMaterial.PLAYER_HEAD) {
            return;
        }

        CustomBlock customBlock = CustomBlock.of(itemStack);
        if (customBlock == null) {
            return;
        }

        // Placing a custom block fires physics updates; toggle physics on for the placement if the world disables it,
        // otherwise the block (e.g. a portal or piston head) would pop off.
        boolean hadToDisablePhysics = false;
        if (isBuildWorld && !buildWorld.getData().get(WorldDataKey.PHYSICS)) {
            hadToDisablePhysics = true;
            buildWorld.getData().set(WorldDataKey.PHYSICS, true);
        }

        setBlock(event, customBlock);

        if (isBuildWorld && hadToDisablePhysics) {
            buildWorld.getData().set(WorldDataKey.PHYSICS, false);
        }
    }

    /**
     * Replaces the placed player head with the custom block on the next tick, cancelling the original placement once the
     * block is in. The block data must be applied a tick later, after Bukkit finishes placing the head.
     *
     * @param event The placement that dropped the custom-block head
     * @param customBlock The custom block to materialize
     */
    public void setBlock(BlockPlaceEvent event, CustomBlock customBlock) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (placer.place(customBlock, block, player)) {
                event.setCancelled(true);
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
