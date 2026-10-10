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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XTag;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.listener.settings.SettingInteractionListener.SettingHandler;
import de.eintosti.buildsystem.util.DirectionUtil;
import de.eintosti.buildsystem.util.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class InstantSignPlacementHandler implements SettingHandler {

    @Override
    public @Nullable Runnable claim(PlayerInteractEvent event, Block block, Settings settings) {
        ItemStack itemStack = event.getItem();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !settings.isInstantPlaceSigns() || itemStack == null) {
            return null;
        }

        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if ((!XTag.SIGNS.isTagged(xMaterial) && !XTag.HANGING_SIGNS.isTagged(xMaterial))
                || !block.getRelative(event.getBlockFace()).getType().isAir()) {
            return null;
        }
        return () -> place(event, block, itemStack.getType(), xMaterial);
    }

    private void place(PlayerInteractEvent event, Block clickedBlock, Material material, XMaterial xMaterial) {
        Player player = event.getPlayer();
        BlockFace blockFace = event.getBlockFace();
        Block adjacent = clickedBlock.getRelative(blockFace);

        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        boolean isHangingSign = XTag.HANGING_SIGNS.isTagged(xMaterial);

        switch (blockFace) {
            case UP -> {
                if (isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                DirectionUtil.rotateBlock(
                        adjacent, DirectionUtil.getPlayerDirection(player).getOppositeFace());
            }
            case DOWN -> {
                if (!isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                DirectionUtil.rotateBlock(adjacent, getHangingSignDirection(event));
            }
            case NORTH, EAST, SOUTH, WEST -> {
                Material wallSign = MaterialUtils.wallVariant(material);
                if (wallSign == null) {
                    return;
                }
                adjacent.setType(wallSign);
                DirectionUtil.rotateBlock(adjacent, isHangingSign ? getHangingSignDirection(event) : blockFace);
            }
        }
    }

    private BlockFace getHangingSignDirection(PlayerInteractEvent event) {
        BlockFace clickedFace = event.getBlockFace();
        BlockFace playerFacing =
                DirectionUtil.getCardinalDirection(event.getPlayer()).getOppositeFace();
        if (clickedFace != playerFacing && clickedFace != playerFacing.getOppositeFace()) {
            return playerFacing;
        }
        return (clickedFace == BlockFace.NORTH || clickedFace == BlockFace.SOUTH) ? BlockFace.EAST : BlockFace.SOUTH;
    }
}
