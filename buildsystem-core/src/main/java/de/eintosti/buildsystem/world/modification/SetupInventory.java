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
package de.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.data.WorldStatus;
import de.eintosti.buildsystem.world.data.WorldType;
import de.eintosti.buildsystem.world.display.WorldIcon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SetupInventory implements Listener {

    private final WorldIcon worldIcon;

    public SetupInventory(BuildSystem plugin) {
        this.worldIcon = plugin.getWorldIcon();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("setup_title", player));
        fillGuiWithGlass(player, inventory);

        InventoryUtils.addSkull(inventory, 10, Messages.getString("setup_create_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_create_item_lore", player));
        InventoryUtils.addSkull(inventory, 19, Messages.getString("setup_default_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_default_item_lore", player));
        InventoryUtils.addSkull(inventory, 28, Messages.getString("setup_status_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_status_item_name_lore", player));

        InventoryUtils.addItem(inventory, 20, worldIcon.getIcon(WorldType.NORMAL), Messages.getString("setup_normal_world", player));
        InventoryUtils.addItem(inventory, 21, worldIcon.getIcon(WorldType.FLAT), Messages.getString("setup_flat_world", player));
        InventoryUtils.addItem(inventory, 22, worldIcon.getIcon(WorldType.NETHER), Messages.getString("setup_nether_world", player));
        InventoryUtils.addItem(inventory, 23, worldIcon.getIcon(WorldType.END), Messages.getString("setup_end_world", player));
        InventoryUtils.addItem(inventory, 24, worldIcon.getIcon(WorldType.VOID), Messages.getString("setup_void_world", player));
        InventoryUtils.addItem(inventory, 25, worldIcon.getIcon(WorldType.IMPORTED), Messages.getString("setup_imported_world", player));

        InventoryUtils.addItem(inventory, 29, worldIcon.getIcon(WorldStatus.NOT_STARTED), Messages.getString("status_not_started", player));
        InventoryUtils.addItem(inventory, 30, worldIcon.getIcon(WorldStatus.IN_PROGRESS), Messages.getString("status_in_progress", player));
        InventoryUtils.addItem(inventory, 31, worldIcon.getIcon(WorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished", player));
        InventoryUtils.addItem(inventory, 32, worldIcon.getIcon(WorldStatus.FINISHED), Messages.getString("status_finished", player));
        InventoryUtils.addItem(inventory, 33, worldIcon.getIcon(WorldStatus.ARCHIVE), Messages.getString("status_archive", player));
        InventoryUtils.addItem(inventory, 34, worldIcon.getIcon(WorldStatus.HIDDEN), Messages.getString("status_hidden", player));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!InventoryUtils.isValidClick(event, "setup_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        InventoryType type = event.getInventory().getType();
        int slot = event.getRawSlot();

        switch (action) {
            case PICKUP_ALL:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case PICKUP_HALF:
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
            case SWAP_WITH_CURSOR:
                if (type != InventoryType.CHEST) {
                    return;
                }

                event.setCancelled(slot < 45 || slot > 80);
                if (action != InventoryAction.SWAP_WITH_CURSOR) {
                    return;
                }

                if (!(slot >= 45 && slot <= 80)) {
                    if ((slot >= 11 && slot <= 15) || (slot >= 20 && slot <= 25) || (slot >= 29 && slot <= 34)) {
                        ItemStack itemStack = event.getCursor();
                        event.setCurrentItem(itemStack);
                        player.setItemOnCursor(null);
                    }
                }
                break;
            default:
                event.setCancelled(true);
                break;
        }
    }
}