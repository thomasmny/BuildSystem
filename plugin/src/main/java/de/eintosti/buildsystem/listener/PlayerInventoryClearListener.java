package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.event.PlayerInventoryClearEvent;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * @author einTosti
 */
public class PlayerInventoryClearListener implements Listener {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;

    public PlayerInventoryClearListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInventoryClear(PlayerInventoryClearEvent event) {
        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);

        if (player.getInventory().getSize() > 0) return;
        if (!settings.isKeepNavigator()) return;
        if (!player.hasPermission("buildsystem.gui")) return;

        PlayerInventory playerInventory = player.getInventory();
        ItemStack navigatorItem = inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item"));
        if (!playerInventory.contains(navigatorItem)) {
            return;
        }

        playerInventory.setItem(8, navigatorItem);
    }
}
