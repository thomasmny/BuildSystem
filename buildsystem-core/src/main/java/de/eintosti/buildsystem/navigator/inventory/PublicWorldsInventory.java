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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.world.display.FolderImpl;
import java.util.AbstractMap;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PublicWorldsInventory extends FilteredWorldsInventory {

    private static final Visibility VISIBILITY = Visibility.PUBLIC;
    private static final Set<BuildWorldStatus> VALID_STATUS = Sets.newHashSet(
            BuildWorldStatus.NOT_STARTED, BuildWorldStatus.IN_PROGRESS, BuildWorldStatus.ALMOST_FINISHED, BuildWorldStatus.FINISHED
    );

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerManager;

    public PublicWorldsInventory(BuildSystemPlugin plugin) {
        super(plugin, "world_navigator_title", "world_navigator_no_worlds", VISIBILITY, VALID_STATUS);

        this.plugin = plugin;
        this.playerManager = plugin.getPlayerService();
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = super.createInventory(player);
        if (playerManager.canCreateWorld(player, super.getVisibility())) {
            addWorldCreateItem(inventory, player);
        }
        addFolderCreateItem(inventory, player);
        return inventory;
    }

    private void addWorldCreateItem(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.create.public")) {
            inventory.setItem(49, InventoryUtils.createSkull(Messages.getString("world_navigator_create_world", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716")));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 49);
        }
    }

    private void addFolderCreateItem(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.folder.create")) {
            inventory.setItem(48, InventoryUtils.createSkull(Messages.getString("world_navigator_create_folder", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158")));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 48);
        }
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        super.onInventoryClick(event);

        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getCurrentItem();

        if (event.getSlot() == 48 && player.hasPermission("buildsystem.folder.create")) {
            player.closeInventory();
            new PlayerChatInput(plugin, player, "enter_folder_name", input -> {
                String folderName = input.trim();
                if (folderName.isEmpty()) {
                    Messages.sendMessage(player, "folder_name_empty");
                    return;
                }

                // Check if folder with same name exists
                if (plugin.getWorldService().getFolderStorage().folderExists(folderName)) {
                    Messages.sendMessage(player, "folder_already_exists");
                    return;
                }

                FolderImpl newFolder = new FolderImpl(folderName);
                plugin.getWorldService().getFolderStorage().addFolder(newFolder);
                Messages.sendMessage(player, "folder_created",
                        new AbstractMap.SimpleEntry<>("%folder%", folderName)
                );
                openInventory(player);
            });
        }
    }
}