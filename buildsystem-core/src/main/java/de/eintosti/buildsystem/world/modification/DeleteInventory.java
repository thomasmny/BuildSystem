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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.Map;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class DeleteInventory implements Listener {

    private final BuildSystemPlugin plugin;

    public DeleteInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player, BuildWorld buildWorld) {
        Inventory inventory = Bukkit.createInventory(null, 27, Messages.getString("delete_title", player));
        fillGuiWithGlass(inventory);

        inventory.setItem(11, InventoryUtils.createItem(XMaterial.LIME_DYE, Messages.getString("delete_world_confirm", player)));
        inventory.setItem(13, createWorldInfo(buildWorld, player));
        inventory.setItem(15, InventoryUtils.createItem(XMaterial.RED_DYE, Messages.getString("delete_world_cancel", player)));

        return inventory;
    }

    private ItemStack createWorldInfo(BuildWorld buildWorld, Player player) {
        ItemStack itemStack = InventoryUtils.createItem(XMaterial.FILLED_MAP,
                Messages.getString("delete_world_name", player, Map.entry("%world%", buildWorld.getName())),
                Messages.getStringList("delete_world_name_lore", player)
        );
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(InventoryUtils.DISPLAYABLE_NAME_KEY, PersistentDataType.STRING, buildWorld.getName());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        player.openInventory(getInventory(player, buildWorld));
    }

    private void fillGuiWithGlass(Inventory inventory) {
        final int[] greenSlots = {0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21};
        final int[] blackSlots = {4, 22};
        final int[] redSlots = {5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26};

        IntStream.of(greenSlots).forEach(slot -> inventory.setItem(slot, InventoryUtils.createItem(XMaterial.LIME_STAINED_GLASS_PANE, "§f")));
        IntStream.of(blackSlots).forEach(slot -> inventory.setItem(slot, InventoryUtils.createItem(XMaterial.BLACK_STAINED_GLASS_PANE, "§f")));
        IntStream.of(redSlots).forEach(slot -> inventory.setItem(slot, InventoryUtils.createItem(XMaterial.RED_STAINED_GLASS_PANE, "§f")));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!InventoryUtils.isValidClick(event, Messages.getString("delete_title", player))) {
            return;
        }

        String worldName = event.getInventory().getItem(13).getItemMeta().getPersistentDataContainer().get(InventoryUtils.DISPLAYABLE_NAME_KEY, PersistentDataType.STRING);
        WorldServiceImpl worldService = plugin.getWorldService();
        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_delete_error");
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 11 -> {
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                player.closeInventory();
                worldService.deleteWorld(player, buildWorld);
            }
            case 15 -> {
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                player.closeInventory();
                Messages.sendMessage(player, "worlds_delete_canceled", Map.entry("%world%", buildWorld.getName()));
            }
        }
    }
}