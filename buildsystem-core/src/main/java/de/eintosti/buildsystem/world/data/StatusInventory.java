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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.inventory.XInventoryView;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.InventoryUtils;
import java.util.AbstractMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StatusInventory implements Listener {

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerService;

    public StatusInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerService = plugin.getPlayerService();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        String selectedWorldName = playerService.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            selectedWorldName = "N/A";
        }

        String title = Messages.getString("status_title", player, new AbstractMap.SimpleEntry<>("%world%", selectedWorldName));
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        fillGuiWithGlass(player, inventory);

        addStatusItem(player, inventory, 10, BuildWorldStatus.NOT_STARTED);
        addStatusItem(player, inventory, 11, BuildWorldStatus.IN_PROGRESS);
        addStatusItem(player, inventory, 12, BuildWorldStatus.ALMOST_FINISHED);
        addStatusItem(player, inventory, 13, BuildWorldStatus.FINISHED);
        addStatusItem(player, inventory, 14, BuildWorldStatus.ARCHIVE);
        addStatusItem(player, inventory, 16, BuildWorldStatus.HIDDEN);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 9; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
        for (int i = 17; i <= 26; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
    }

    private void addStatusItem(Player player, Inventory inventory, int position, BuildWorldStatus status) {
        XMaterial material = plugin.getCustomizableIcons().getIcon(status);
        String displayName = Messages.getString(status.getMessageKey(), player);

        if (!player.hasPermission(status.getPermission())) {
            material = XMaterial.BARRIER;
            displayName = "§c§m" + ChatColor.stripColor(displayName);
        }

        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);

        BuildWorld cachedWorld = playerService.getPlayerStorage().getBuildPlayer(player).getCachedWorld();
        if (cachedWorld != null && cachedWorld.getData().status().get() == status) {
            itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
        }

        inventory.setItem(position, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String selectedWorldName = playerService.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            return;
        }

        String statusTitle = Messages.getString("status_title", player, new AbstractMap.SimpleEntry<>("%world%", selectedWorldName));
        if (!XInventoryView.of(event.getView()).getTitle().equals(statusTitle)) {
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

        BuildPlayer buildPlayer = playerService.getPlayerStorage().getBuildPlayer(player);
        BuildWorld buildWorld = buildPlayer.getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_setstatus_error");
            return;
        }

        int slot = event.getSlot();
        if (slot < 10 || slot > 14 && slot != 16) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            plugin.getEditInventory().openInventory(player, buildWorld);
            return;
        }

        BuildWorldStatus status = getStatusFromSlot(slot);
        if (!player.hasPermission(status.getPermission())) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        player.closeInventory();
        buildPlayer.setCachedWorld(null);
        buildWorld.getData().status().set(status);
        playerService.forceUpdateSidebar(buildWorld);

        XSound.ENTITY_CHICKEN_EGG.play(player);
        Messages.sendMessage(player, "worlds_setstatus_set",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()),
                new AbstractMap.SimpleEntry<>("%status%", Messages.getString(status.getMessageKey(), player))
        );
    }

    /**
     * Gets the {@link BuildWorldStatus} which is represented by the item at the given slot.
     *
     * @param slot The slot to get the status from
     * @return The status which is represented by the item at the given slot
     */
    private BuildWorldStatus getStatusFromSlot(int slot) {
        switch (slot) {
            case 10:
                return BuildWorldStatus.NOT_STARTED;
            case 11:
                return BuildWorldStatus.IN_PROGRESS;
            case 12:
                return BuildWorldStatus.ALMOST_FINISHED;
            case 13:
                return BuildWorldStatus.FINISHED;
            case 14:
                return BuildWorldStatus.ARCHIVE;
            case 16:
                return BuildWorldStatus.HIDDEN;
            default:
                throw new IllegalArgumentException("Slot " + slot + " does not correspond to status");
        }
    }
}