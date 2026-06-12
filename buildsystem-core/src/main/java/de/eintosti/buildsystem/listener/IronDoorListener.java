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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class IronDoorListener implements Listener {

    private final SettingsService settingsManager;
    private final WorldStorageImpl worldStorage;
    private final WorldProtectionPolicy policy;

    public IronDoorListener(BuildSystemPlugin plugin) {
        this.settingsManager = plugin.getSettingsService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.policy = new WorldProtectionPolicy();
    }

    @EventHandler
    public void manageIronDoorSetting(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!settingsManager.getSettings(player).isOpenTrapDoors()) {
            return;
        }

        XMaterial material = XMaterial.matchXMaterial(block.getType());
        if (material != XMaterial.IRON_DOOR && material != XMaterial.IRON_TRAPDOOR) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
        if (buildWorld != null
                && policy.mayModify(player, buildWorld, buildWorld.getData().blockPlacement()) != Denial.NONE) {
            return;
        }

        event.setCancelled(true);
        Openable openable = (Openable) block.getBlockData();
        openable.setOpen(!openable.isOpen());
        block.setBlockData(openable);
    }
}
