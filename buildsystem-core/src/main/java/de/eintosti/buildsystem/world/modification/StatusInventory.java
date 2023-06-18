/*
 * Copyright (c) 2018-2023, Thomas Meaney
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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldStatus;
import de.eintosti.buildsystem.player.BuildPlayerManager;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.CraftBuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;

public class StatusInventory implements Listener {

    private final BuildSystemPlugin plugin;
    private final InventoryUtils inventoryUtils;
    private final BuildPlayerManager playerManager;

    public StatusInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        String selectedWorldName = playerManager.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            selectedWorldName = "N/A";
        }

        String title = Messages.getString("status_title", new AbstractMap.SimpleEntry<>("%world%", selectedWorldName));
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        fillGuiWithGlass(player, inventory);

        addItem(player, inventory, 10, inventoryUtils.getStatusItem(WorldStatus.NOT_STARTED), Messages.getString("status_not_started"), WorldStatus.NOT_STARTED);
        addItem(player, inventory, 11, inventoryUtils.getStatusItem(WorldStatus.IN_PROGRESS), Messages.getString("status_in_progress"), WorldStatus.IN_PROGRESS);
        addItem(player, inventory, 12, inventoryUtils.getStatusItem(WorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished"), WorldStatus.ALMOST_FINISHED);
        addItem(player, inventory, 13, inventoryUtils.getStatusItem(WorldStatus.FINISHED), Messages.getString("status_finished"), WorldStatus.FINISHED);
        addItem(player, inventory, 14, inventoryUtils.getStatusItem(WorldStatus.ARCHIVE), Messages.getString("status_archive"), WorldStatus.ARCHIVE);
        addItem(player, inventory, 16, inventoryUtils.getStatusItem(WorldStatus.HIDDEN), Messages.getString("status_hidden"), WorldStatus.HIDDEN);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 9; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 17; i <= 26; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addItem(Player player, Inventory inventory, int position, XMaterial material, String displayName, WorldStatus worldStatus) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        itemStack.setItemMeta(itemMeta);

        CraftBuildWorld cachedWorld = playerManager.getBuildPlayer(player).getCachedWorld();
        if (cachedWorld != null && cachedWorld.getData().status().get() == worldStatus) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String selectedWorldName = playerManager.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            return;
        }

        String title = Messages.getString("status_title", new AbstractMap.SimpleEntry<>("%world%", selectedWorldName));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }

        event.setCancelled(true);
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Material itemType = itemStack.getType();
        if (itemType == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        CraftBuildWorld buildWorld = playerManager.getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_setstatus_error");
            return;
        }

        WorldData worldData = buildWorld.getData();
        switch (event.getSlot()) {
            case 10:
                worldData.status().set(WorldStatus.NOT_STARTED);
                break;
            case 11:
                worldData.status().set(WorldStatus.IN_PROGRESS);
                break;
            case 12:
                worldData.status().set(WorldStatus.ALMOST_FINISHED);
                break;
            case 13:
                worldData.status().set(WorldStatus.FINISHED);
                break;
            case 14:
                worldData.status().set(WorldStatus.ARCHIVE);
                break;
            case 16:
                worldData.status().set(WorldStatus.HIDDEN);
                break;
            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getEditInventory().openInventory(player, buildWorld);
                return;
        }

        playerManager.forceUpdateSidebar(buildWorld);
        player.closeInventory();

        XSound.ENTITY_CHICKEN_EGG.play(player);
        Messages.sendMessage(player, "worlds_setstatus_set",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()),
                new AbstractMap.SimpleEntry<>("%status%", Messages.getDataString(buildWorld.getData().status().get().getKey()))
        );
        playerManager.getBuildPlayer(player).setCachedWorld(null);
    }
}