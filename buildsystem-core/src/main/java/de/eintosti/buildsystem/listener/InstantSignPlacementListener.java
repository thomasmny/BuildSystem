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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XTag;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.DirectionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InstantSignPlacementListener implements Listener {

    private final CustomBlockManager customBlockManager;
    private final SettingsService settingsManager;
    private final WorldStorageImpl worldStorage;
    private final WorldProtectionPolicy policy;

    public InstantSignPlacementListener(BuildSystemPlugin plugin) {
        this.customBlockManager = plugin.getCustomBlockManager();
        this.settingsManager = plugin.getSettingsService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.policy = new WorldProtectionPolicy();
    }

    @EventHandler
    public void manageInstantPlaceSignsSetting(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        if (!settingsManager.getSettings(player).isInstantPlaceSigns()) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (!XTag.SIGNS.isTagged(xMaterial) && !XTag.HANGING_SIGNS.isTagged(xMaterial)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        if (clickedBlock == null) {
            return;
        }

        Block adjacent = clickedBlock.getRelative(blockFace);
        if (adjacent.getType() != XMaterial.AIR.get()) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
        if (buildWorld != null
                && policy.mayModify(player, buildWorld, buildWorld.getData().blockPlacement()) != Denial.NONE) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        boolean isHangingSign = XTag.HANGING_SIGNS.isTagged(xMaterial);

        switch (blockFace) {
            case UP -> {
                if (isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                customBlockManager.rotateBlock(
                        adjacent, DirectionUtil.getPlayerDirection(player).getOppositeFace());
            }
            case DOWN -> {
                if (!isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                customBlockManager.rotateBlock(adjacent, getHangingSignDirection(event));
            }
            case NORTH, EAST, SOUTH, WEST -> {
                String woodType = xMaterial.name().replace("_HANGING", "").replace("_SIGN", "");
                String block = isHangingSign ? "_WALL_HANGING_SIGN" : "_WALL_SIGN";
                BlockFace facing = isHangingSign ? getHangingSignDirection(event) : blockFace;
                XMaterial.matchXMaterial(woodType + block).ifPresent(value -> adjacent.setType(value.get()));
                customBlockManager.rotateBlock(adjacent, facing);
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
