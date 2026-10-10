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
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.util.DirectionUtil;
import de.eintosti.buildsystem.util.MaterialUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DisabledInteractionsListener implements Listener {

    private final SettingsService settingsManager;
    private final WorldStorage worldStorage;
    private final ConfigService configService;
    private final WorldProtectionPolicy policy;

    public DisabledInteractionsListener(
            SettingsService settingsManager, WorldStorage worldStorage, ConfigService configService) {
        this.settingsManager = settingsManager;
        this.worldStorage = worldStorage;
        this.configService = configService;
        this.policy = new WorldProtectionPolicy();
    }

    @EventHandler
    public void manageDisabledInteractSetting(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.isCancelled()) {
            return;
        }

        if (!settingsManager.getSettings(player).isDisableInteract()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !block.getType().isInteractable()) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();
        XMaterial xMaterial = XMaterial.matchXMaterial(material);
        if (xMaterial == configService.current().settings().builder().worldEditWand()) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld != null && policy.mayModify(player, buildWorld, WorldSetting.BLOCK_PLACEMENT) != Denial.NONE) {
            return;
        }

        // Cancelling denies the interacted block too, which is what keeps a container from opening.
        event.setCancelled(true);

        Material placed = XTag.SIGNS.isTagged(xMaterial) && event.getBlockFace() != BlockFace.UP
                ? MaterialUtils.wallVariant(material)
                : material;
        if (placed == null || !placed.isBlock()) {
            return;
        }

        Block adjacent = block.getRelative(event.getBlockFace());
        BlockState replaced = adjacent.getState();
        adjacent.setType(placed);
        DirectionUtil.rotateBlock(adjacent, DirectionUtil.getBlockDirection(player, false));

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            hand = EquipmentSlot.HAND;
        }

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(adjacent, replaced, block, itemStack, player, true, hand);
        Bukkit.getServer().getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled()) {
            replaced.update(true, false);
        }
    }
}
