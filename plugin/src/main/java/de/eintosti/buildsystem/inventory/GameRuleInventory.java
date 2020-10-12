package de.eintosti.buildsystem.inventory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.version.GameRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * @author einTosti
 */
public class GameRuleInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final GameRules gameRules;

    public GameRuleInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.gameRules = plugin.getGameRules();
    }

    public void openInventory(Player player, World world) {
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());

        Inventory inventory = gameRules.getInventory(player, bukkitWorld);
        fillGuiWithGlass(player, inventory);

        player.openInventory(inventory);
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isValidSlot(i)) {
                inventoryManager.addGlassPane(plugin, player, inventory, i);
            }
        }

        UUID playerUUID = player.getUniqueId();
        int numGameRules = gameRules.getNumGameRules();
        int invIndex = gameRules.getInvIndex(playerUUID);

        if (numGameRules > 1 && invIndex > 0) {
            inventoryManager.addUrlSkull(inventory, 36, plugin.getString("gui_previous_page"), "http://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 36);
        }

        if (numGameRules > 1 && invIndex < (numGameRules - 1)) {
            inventoryManager.addUrlSkull(inventory, 44, plugin.getString("gui_next_page"), "http://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 44);
        }
    }

    private boolean isValidSlot(int slot) {
        int[] slots = gameRules.getSlots();
        for (int i : slots) {
            if (i == slot) {
                return false;
            }
        }
        return true;
    }
}
