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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
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

    private final CustomizableIcons worldIcon;

    public SetupInventory(BuildSystemPlugin plugin) {
        this.worldIcon = plugin.getCustomizableIcons();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Messages.getString("setup_title", player));
        fillGuiWithGlass(player, inventory);

        inventory.setItem(10, InventoryUtils.createSkull(Messages.getString("setup_default_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_default_item_lore", player)));
        inventory.setItem(19, InventoryUtils.createSkull(Messages.getString("setup_status_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_status_item_name_lore", player)));

        inventory.setItem(11, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldType.NORMAL), Messages.getString("setup_normal_world", player)));
        inventory.setItem(12, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldType.FLAT), Messages.getString("setup_flat_world", player)));
        inventory.setItem(13, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldType.NETHER), Messages.getString("setup_nether_world", player)));
        inventory.setItem(14, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldType.END), Messages.getString("setup_end_world", player)));
        inventory.setItem(15, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldType.VOID), Messages.getString("setup_void_world", player)));
        inventory.setItem(16, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldType.IMPORTED), Messages.getString("setup_imported_world", player)));

        inventory.setItem(20, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldStatus.NOT_STARTED), Messages.getString("status_not_started", player)));
        inventory.setItem(21, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldStatus.IN_PROGRESS), Messages.getString("status_in_progress", player)));
        inventory.setItem(22, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished", player)));
        inventory.setItem(23, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldStatus.FINISHED), Messages.getString("status_finished", player)));
        inventory.setItem(24, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldStatus.ARCHIVE), Messages.getString("status_archive", player)));
        inventory.setItem(25, InventoryUtils.createItem(worldIcon.getIcon(BuildWorldStatus.HIDDEN), Messages.getString("status_hidden", player)));

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
        Player player = (Player) event.getWhoClicked();
        if (!InventoryUtils.isValidClick(event, Messages.getString("setup_title", player))) {
            return;
        }

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

                event.setCancelled(slot < 36 || slot > 80);
                if (action != InventoryAction.SWAP_WITH_CURSOR) {
                    return;
                }

                if (!(slot >= 36 && slot <= 80)) {
                    if ((slot >= 11 && slot <= 15) || (slot >= 20 && slot <= 25)) {
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