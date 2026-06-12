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

import com.cryptomorin.xseries.XBlock;
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
import de.eintosti.buildsystem.util.MaterialUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DisabledInteractionsListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final CustomBlockManager customBlockManager;
    private final SettingsService settingsManager;
    private final WorldStorageImpl worldStorage;
    private final WorldProtectionPolicy policy;
    private final Set<UUID> cachePlayers;

    public DisabledInteractionsListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.customBlockManager = plugin.getCustomBlockManager();
        this.settingsManager = plugin.getSettingsService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.policy = new WorldProtectionPolicy();
        this.cachePlayers = new HashSet<>();
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
        if (block == null || !MaterialUtils.isInteractable(XMaterial.matchXMaterial(block.getType()))) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();
        XMaterial xMaterial = XMaterial.matchXMaterial(material);
        if (xMaterial
                == plugin.getConfigService().current().settings().builder().worldEditWand()) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
        if (buildWorld != null
                && policy.mayModify(player, buildWorld, buildWorld.getData().blockPlacement()) != Denial.NONE) {
            return;
        }

        cachePlayers.add(player.getUniqueId());
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        if (XTag.SIGNS.isTagged(xMaterial) && event.getBlockFace() != BlockFace.UP) {
            String[] splitMaterial = material.toString().split("_");
            material = Material.valueOf(splitMaterial[0] + "_WALL_SIGN");
        }

        if (!material.isBlock()) {
            return;
        }

        Block adjacent = block.getRelative(event.getBlockFace());
        adjacent.setType(material);
        XBlock.setColor(adjacent, DyeColor.getByWoolData((byte) itemStack.getDurability()));

        customBlockManager.rotateBlock(adjacent, DirectionUtil.getBlockDirection(player, false));

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            hand = EquipmentSlot.HAND;
        }

        Bukkit.getServer()
                .getPluginManager()
                .callEvent(new BlockPlaceEvent(adjacent, adjacent.getState(), block, itemStack, player, true, hand));
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (cachePlayers.remove(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
