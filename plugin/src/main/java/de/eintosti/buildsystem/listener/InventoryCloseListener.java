package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.object.world.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public class InventoryCloseListener implements Listener {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final WorldManager worldManager;

    public InventoryCloseListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPrivateInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        worldManager.createPrivateWorldPlayers.remove(player);
    }

    @EventHandler
    public void onSetupInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("setup_title"))) return;
        setNewItems(event);
    }

    private void setNewItems(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        ItemStack normalCreateItem = inventory.getItem(11);
        ItemStack flatCreateItem = inventory.getItem(12);
        ItemStack netherCreateItem = inventory.getItem(13);
        ItemStack endCreateItem = inventory.getItem(14);
        ItemStack voidCreateItem = inventory.getItem(15);

        inventoryManager.setCreateItem(WorldType.NORMAL, normalCreateItem != null ? XMaterial.matchXMaterial(normalCreateItem) : null);
        inventoryManager.setCreateItem(WorldType.FLAT, flatCreateItem != null ? XMaterial.matchXMaterial(flatCreateItem) : null);
        inventoryManager.setCreateItem(WorldType.NETHER, netherCreateItem != null ? XMaterial.matchXMaterial(netherCreateItem) : null);
        inventoryManager.setCreateItem(WorldType.END, endCreateItem != null ? XMaterial.matchXMaterial(endCreateItem) : null);
        inventoryManager.setCreateItem(WorldType.VOID, voidCreateItem != null ? XMaterial.matchXMaterial(voidCreateItem) : null);

        ItemStack normalDefaultItem = inventory.getItem(20);
        ItemStack flatDefaultItem = inventory.getItem(21);
        ItemStack netherDefaultItem = inventory.getItem(22);
        ItemStack endDefaultItem = inventory.getItem(23);
        ItemStack voidDefaultItem = inventory.getItem(24);
        ItemStack importedDefaultItem = inventory.getItem(25);

        inventoryManager.setDefaultItem(WorldType.NORMAL, normalDefaultItem != null ? XMaterial.matchXMaterial(normalDefaultItem) : null);
        inventoryManager.setDefaultItem(WorldType.FLAT, flatDefaultItem != null ? XMaterial.matchXMaterial(flatDefaultItem) : null);
        inventoryManager.setDefaultItem(WorldType.NETHER, netherDefaultItem != null ? XMaterial.matchXMaterial(netherDefaultItem) : null);
        inventoryManager.setDefaultItem(WorldType.END, endDefaultItem != null ? XMaterial.matchXMaterial(endDefaultItem) : null);
        inventoryManager.setDefaultItem(WorldType.VOID, voidDefaultItem != null ? XMaterial.matchXMaterial(voidDefaultItem) : null);
        inventoryManager.setDefaultItem(WorldType.IMPORTED, importedDefaultItem != null ? XMaterial.matchXMaterial(importedDefaultItem) : null);

        ItemStack notStartedStatusItem = inventory.getItem(29);
        ItemStack inProgressStatusItem = inventory.getItem(30);
        ItemStack almostFinishedStatusItem = inventory.getItem(31);
        ItemStack finishedStatusItem = inventory.getItem(32);
        ItemStack archiveStatusItem = inventory.getItem(33);
        ItemStack hiddenStatusItem = inventory.getItem(34);

        inventoryManager.setStatusItem(WorldStatus.NOT_STARTED, notStartedStatusItem != null ? XMaterial.matchXMaterial(notStartedStatusItem) : null);
        inventoryManager.setStatusItem(WorldStatus.IN_PROGRESS, inProgressStatusItem != null ? XMaterial.matchXMaterial(inProgressStatusItem) : null);
        inventoryManager.setStatusItem(WorldStatus.ALMOST_FINISHED, almostFinishedStatusItem != null ? XMaterial.matchXMaterial(almostFinishedStatusItem) : null);
        inventoryManager.setStatusItem(WorldStatus.FINISHED, finishedStatusItem != null ? XMaterial.matchXMaterial(finishedStatusItem) : null);
        inventoryManager.setStatusItem(WorldStatus.ARCHIVE, archiveStatusItem != null ? XMaterial.matchXMaterial(archiveStatusItem) : null);
        inventoryManager.setStatusItem(WorldStatus.HIDDEN, hiddenStatusItem != null ? XMaterial.matchXMaterial(hiddenStatusItem) : null);
    }
}
