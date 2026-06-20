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
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.util.DirectionUtil;
import java.util.Arrays;
import java.util.EnumSet;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlantPlacementListener implements Listener {

    private static final EnumSet<XMaterial> OTHER_PLANTS = Sets.newEnumSet(
            Sets.newHashSet(
                    XMaterial.TORCHFLOWER,
                    XMaterial.PITCHER_PLANT,
                    XMaterial.LILY_PAD,
                    XMaterial.PINK_PETALS,
                    XMaterial.BROWN_MUSHROOM,
                    XMaterial.RED_MUSHROOM,
                    XMaterial.CRIMSON_FUNGUS,
                    XMaterial.WARPED_FUNGUS,
                    XMaterial.SHORT_GRASS,
                    XMaterial.FERN,
                    XMaterial.DEAD_BUSH,
                    XMaterial.LARGE_FERN,
                    XMaterial.TALL_GRASS,
                    XMaterial.NETHER_SPROUTS,
                    XMaterial.WARPED_ROOTS,
                    XMaterial.CRIMSON_ROOTS,
                    XMaterial.SUGAR_CANE,
                    XMaterial.BAMBOO,
                    XMaterial.BIG_DRIPLEAF,
                    XMaterial.SMALL_DRIPLEAF,
                    XMaterial.SEAGRASS,
                    XMaterial.SWEET_BERRIES),
            XMaterial.class);

    private final SettingsService settingsManager;
    private final WorldStorage worldStorage;
    private final WorldProtectionPolicy policy;

    public PlantPlacementListener(SettingsService settingsManager, WorldStorage worldStorage) {
        this.settingsManager = settingsManager;
        this.worldStorage = worldStorage;
        this.policy = new WorldProtectionPolicy();
    }

    @EventHandler
    public void managePlacePlantsSetting(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!settingsManager.getSettings(player).isPlacePlants()) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack.getType());
        if (!XTag.FLOWERS.isTagged(xMaterial)
                && !XTag.REPLACEABLE_PLANTS.isTagged(xMaterial)
                && !XTag.ALIVE_CORAL_PLANTS.isTagged(xMaterial)
                && !XTag.DEAD_CORAL_PLANTS.isTagged(xMaterial)
                && !XTag.SAPLINGS.isTagged(xMaterial)
                && !OTHER_PLANTS.contains(xMaterial)) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
        if (buildWorld != null && policy.mayModify(player, buildWorld, WorldSetting.BLOCK_PLACEMENT) != Denial.NONE) {
            return;
        }

        event.setCancelled(true);
        Block adjacent = block.getRelative(event.getBlockFace());

        switch (xMaterial) {
            case SWEET_BERRIES:
                adjacent.setType(XMaterial.SWEET_BERRY_BUSH.get());
                break;
            case VINE:
                BlockFace toPlace = event.getBlockFace().getOppositeFace();
                if (toPlace == BlockFace.DOWN) {
                    break;
                }
                adjacent.setType(xMaterial.get());
                MultipleFacing multipleFacing = (MultipleFacing) adjacent.getBlockData();
                Arrays.stream(DirectionUtil.BLOCK_SIDES)
                        .forEach(blockFace -> multipleFacing.setFace(blockFace, blockFace == toPlace));
                adjacent.setBlockData(multipleFacing);
                break;
            default:
                adjacent.setType(xMaterial.get());
                break;
        }
    }
}
