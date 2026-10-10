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
package de.eintosti.buildsystem.listener.settings;

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.listener.settings.SettingInteractionListener.SettingHandler;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class SlabHandler implements SettingHandler {

    @Override
    public boolean takes(PlayerInteractEvent event, Block block, Settings settings) {
        return event.getAction() == Action.LEFT_CLICK_BLOCK
                && settings.isSlabBreaking()
                && block.getBlockData() instanceof Slab slab
                && slab.getType() == Slab.Type.DOUBLE;
    }

    @Override
    public void handle(PlayerInteractEvent event, Block block) {
        event.setCancelled(true);
        Slab slab = (Slab) block.getBlockData();
        slab.setType(hitsTopHalf(event.getPlayer(), block) ? Slab.Type.BOTTOM : Slab.Type.TOP);
        block.setBlockData(slab);
    }

    /**
     * {@return whether the player is aiming at the upper half of {@code block}} Measured from the block's own height, so
     * it holds below y=0 too.
     */
    static boolean hitsTopHalf(Player player, Block block) {
        RayTraceResult result = player.rayTraceBlocks(6);
        return result != null && result.getHitPosition().getY() - block.getY() >= 0.5;
    }
}
