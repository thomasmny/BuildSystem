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
package de.eintosti.buildsystem.navigator.inventory;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.navigator.Displayable;
import de.eintosti.buildsystem.navigator.NavigatorInventory;
import de.eintosti.buildsystem.navigator.folder.Folder;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.BuildWorld;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DisplayableInventory extends NavigatorInventory {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;
    private final Collection<? extends Displayable> displayables;
    private final int inventorySize;

    public DisplayableInventory(BuildSystem plugin, Collection<? extends Displayable> displayables) {
        super(plugin);
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtils();
        this.displayables = displayables;
        this.inventorySize = calculateInventorySize();
    }

    private int calculateInventorySize() {
        int size = displayables.size() + 9;
        if (size <= 9) {
            return 27;
        }
        if (size <= 18) {
            return 36;
        }
        if (size <= 27) {
            return 45;
        }
        return 54;
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, inventorySize, Messages.getString("navigator_title", player));

        int slot = 0;
        for (Displayable displayable : displayables) {
            if (displayable instanceof BuildWorld) {
                BuildWorld buildWorld = (BuildWorld) displayable;
                List<String> lore = inventoryUtils.getWorldLore(player, buildWorld);
                ItemStack item = inventoryUtils.createWorldItem(buildWorld.getData().getMaterial(), buildWorld.getName(), lore);
                inventory.setItem(slot++, item);
            } else if (displayable instanceof Folder) {
                Folder folder = (Folder) displayable;
                List<String> lore = new ArrayList<>();
                lore.add(Messages.getString("folder_name", player).replace("%folder%", folder.getName()));
                lore.add(Messages.getString("folder_contents", player).replace("%count%", String.valueOf(folder.getContents().size())));
                ItemStack item = inventoryUtils.createItem(XMaterial.CHEST, folder.getName(), lore);
                inventory.setItem(slot++, item);
            }
        }

        inventoryUtils.fillInventory(inventory);
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        String displayName = item.getItemMeta().getDisplayName();
        for (Displayable displayable : displayables) {
            if (displayable.getName().equals(displayName)) {
                if (displayable instanceof BuildWorld) {
                    BuildWorld buildWorld = (BuildWorld) displayable;
                    plugin.getWorldManager().teleportToWorld(player, buildWorld);
                } else if (displayable instanceof Folder) {
                    Folder folder = (Folder) displayable;
                    Navigator navigator = plugin.getNavigator();
                    navigator.openInventory(player, folder.getContents());
                }
                break;
            }
        }
    }
} 