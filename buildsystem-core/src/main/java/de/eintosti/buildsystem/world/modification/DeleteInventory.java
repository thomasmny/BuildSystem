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
import de.eintosti.buildsystem.util.inventory.BuildWorldHolder;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import java.util.Map;
import java.util.stream.IntStream;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class DeleteInventory implements Listener {

    private final BuildSystemPlugin plugin;

    public DeleteInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player, BuildWorld buildWorld) {
        Inventory inventory = new DeleteInventoryHolder(buildWorld, player).getInventory();
        fillGuiWithGlass(inventory);

        inventory.setItem(11, InventoryUtils.createItem(XMaterial.LIME_DYE,
                Messages.getString("delete_world_confirm", player))
        );
        inventory.setItem(13, InventoryUtils.createItem(XMaterial.FILLED_MAP,
                Messages.getString("delete_world_name", player, Map.entry("%world%", buildWorld.getName())),
                Messages.getStringList("delete_world_name_lore", player)
        ));
        inventory.setItem(15, InventoryUtils.createItem(XMaterial.RED_DYE,
                Messages.getString("delete_world_cancel", player))
        );

        return inventory;
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
        if (!(event.getInventory().getHolder() instanceof DeleteInventoryHolder holder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = holder.getBuildWorld();

        switch (event.getSlot()) {
            case 11 -> {
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                player.closeInventory();
                plugin.getWorldService().deleteWorld(player, buildWorld);
            }
            case 15 -> {
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                player.closeInventory();
                Messages.sendMessage(player, "worlds_delete_canceled", Map.entry("%world%", buildWorld.getName()));
            }
        }
    }

    private static class DeleteInventoryHolder extends BuildWorldHolder {

        public DeleteInventoryHolder(BuildWorld buildWorld, Player player) {
            super(buildWorld, 27, Messages.getString("delete_title", player));
        }
    }
}