package de.eintosti.buildsystem.inventory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class DeleteInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public DeleteInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    private Inventory getInventory(Player player, World world) {
        Inventory inventory = Bukkit.createInventory(null, 45, plugin.getString("delete_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addItemStack(inventory, 13, XMaterial.FILLED_MAP, plugin.getString("delete_world_name").replace("%world%", world.getName()), plugin.getStringList("delete_world_name_lore"));
        inventoryManager.addItemStack(inventory, 29, XMaterial.RED_DYE, plugin.getString("delete_world_cancel"));
        inventoryManager.addItemStack(inventory, 33, XMaterial.LIME_DYE, plugin.getString("delete_world_confirm"));

        return inventory;
    }

    public void openInventory(Player player, World world) {
        player.openInventory(getInventory(player, world));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }
}
