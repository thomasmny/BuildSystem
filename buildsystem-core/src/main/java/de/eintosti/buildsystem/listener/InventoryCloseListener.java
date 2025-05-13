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
import com.cryptomorin.xseries.inventory.XInventoryView;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryCloseListener implements Listener {

    public InventoryCloseListener(BuildSystemPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSetupInventoryClose(InventoryCloseEvent event) {
        String title = XInventoryView.of(event.getView()).getTitle();
        if (!title.equals(Messages.getString("setup_title", (Player) event.getPlayer()))) {
            return;
        }
        saveItems(event.getInventory());
    }

    private void saveItems(Inventory inventory) {
        ItemStack normalCreateItem = inventory.getItem(11);
        ItemStack flatCreateItem = inventory.getItem(12);
        ItemStack netherCreateItem = inventory.getItem(13);
        ItemStack endCreateItem = inventory.getItem(14);
        ItemStack voidCreateItem = inventory.getItem(15);

        InventoryUtils.setCreateItem(BuildWorldType.NORMAL,
                normalCreateItem != null ? XMaterial.matchXMaterial(normalCreateItem) : null);
        InventoryUtils.setCreateItem(BuildWorldType.FLAT,
                flatCreateItem != null ? XMaterial.matchXMaterial(flatCreateItem) : null);
        InventoryUtils.setCreateItem(BuildWorldType.NETHER,
                netherCreateItem != null ? XMaterial.matchXMaterial(netherCreateItem) : null);
        InventoryUtils.setCreateItem(BuildWorldType.END,
                endCreateItem != null ? XMaterial.matchXMaterial(endCreateItem) : null);
        InventoryUtils.setCreateItem(BuildWorldType.VOID,
                voidCreateItem != null ? XMaterial.matchXMaterial(voidCreateItem) : null);

        ItemStack normalDefaultItem = inventory.getItem(20);
        ItemStack flatDefaultItem = inventory.getItem(21);
        ItemStack netherDefaultItem = inventory.getItem(22);
        ItemStack endDefaultItem = inventory.getItem(23);
        ItemStack voidDefaultItem = inventory.getItem(24);
        ItemStack importedDefaultItem = inventory.getItem(25);

        InventoryUtils.setDefaultItem(BuildWorldType.NORMAL,
                normalDefaultItem != null ? XMaterial.matchXMaterial(normalDefaultItem) : null);
        InventoryUtils.setDefaultItem(BuildWorldType.FLAT,
                flatDefaultItem != null ? XMaterial.matchXMaterial(flatDefaultItem) : null);
        InventoryUtils.setDefaultItem(BuildWorldType.NETHER,
                netherDefaultItem != null ? XMaterial.matchXMaterial(netherDefaultItem) : null);
        InventoryUtils.setDefaultItem(BuildWorldType.END,
                endDefaultItem != null ? XMaterial.matchXMaterial(endDefaultItem) : null);
        InventoryUtils.setDefaultItem(BuildWorldType.VOID,
                voidDefaultItem != null ? XMaterial.matchXMaterial(voidDefaultItem) : null);
        InventoryUtils.setDefaultItem(BuildWorldType.IMPORTED,
                importedDefaultItem != null ? XMaterial.matchXMaterial(importedDefaultItem) : null);

        ItemStack notStartedStatusItem = inventory.getItem(29);
        ItemStack inProgressStatusItem = inventory.getItem(30);
        ItemStack almostFinishedStatusItem = inventory.getItem(31);
        ItemStack finishedStatusItem = inventory.getItem(32);
        ItemStack archiveStatusItem = inventory.getItem(33);
        ItemStack hiddenStatusItem = inventory.getItem(34);

        InventoryUtils.setStatusItem(WorldStatus.NOT_STARTED,
                notStartedStatusItem != null ? XMaterial.matchXMaterial(notStartedStatusItem) : null);
        InventoryUtils.setStatusItem(WorldStatus.IN_PROGRESS,
                inProgressStatusItem != null ? XMaterial.matchXMaterial(inProgressStatusItem) : null);
        InventoryUtils.setStatusItem(WorldStatus.ALMOST_FINISHED,
                almostFinishedStatusItem != null ? XMaterial.matchXMaterial(almostFinishedStatusItem) : null);
        InventoryUtils.setStatusItem(WorldStatus.FINISHED,
                finishedStatusItem != null ? XMaterial.matchXMaterial(finishedStatusItem) : null);
        InventoryUtils.setStatusItem(WorldStatus.ARCHIVE,
                archiveStatusItem != null ? XMaterial.matchXMaterial(archiveStatusItem) : null);
        InventoryUtils.setStatusItem(WorldStatus.HIDDEN,
                hiddenStatusItem != null ? XMaterial.matchXMaterial(hiddenStatusItem) : null);
    }
}