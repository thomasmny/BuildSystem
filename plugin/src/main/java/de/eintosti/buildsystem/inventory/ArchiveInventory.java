package de.eintosti.buildsystem.inventory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class ArchiveInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final WorldManager worldManager;

    private final Map<UUID, Integer> invIndex;
    private Inventory[] inventories;

    public ArchiveInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.worldManager = plugin.getWorldManager();
        this.invIndex = new HashMap<>();
    }

    private Inventory createInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, plugin.getString("archive_title"));

        int numOfWorlds = (numOfWorlds(player) / 36) + (numOfWorlds(player) % 36 == 0 ? 0 : 1);
        inventoryManager.fillMultiInvWithGlass(plugin, inventory, player, invIndex, numOfWorlds);
        inventoryManager.addGlassPane(plugin, player, inventory, 49);

        return inventory;
    }

    private Inventory getInventory(Player player) {
        if (getInvIndex(player) == null) {
            setInvIndex(player, 0);
        }
        addWorlds(player);
        return inventories[getInvIndex(player)];
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private int numOfWorlds(Player player) {
        int numOfWorlds = 0;
        for (BuildWorld buildWorld : worldManager.getBuildWorlds()) {
            if (isValid(player, buildWorld)) {
                numOfWorlds++;
            }
        }
        return numOfWorlds;
    }

    private void addWorlds(Player player) {
        int columnWorld = 9, maxColumnWorld = 44;
        int numWorlds = numOfWorlds(player);
        int numInventories = (numWorlds % 36 == 0 ? numWorlds : numWorlds + 1) != 0 ? (numWorlds % 36 == 0 ? numWorlds : numWorlds + 1) : 1;

        inventories = new Inventory[numInventories];
        Inventory inventory = createInventory(player);

        int index = 0;
        inventories[index] = inventory;
        if (numWorlds == 0) {
            inventoryManager.addUrlSkull(inventory, 22, plugin.getString("archive_no_worlds"), "http://textures.minecraft.net/texture/2e3f50ba62cbda3ecf5479b62fedebd61d76589771cc19286bf2745cd71e47c6");
            return;
        }

        List<BuildWorld> buildWorlds = inventoryManager.sortWorlds(player, worldManager, plugin);
        for (BuildWorld buildWorld : buildWorlds) {
            if (isValid(player, buildWorld)) {
                inventoryManager.addWorldItem(player, inventory, columnWorld++, buildWorld);
            }
            if (columnWorld > maxColumnWorld) {
                columnWorld = 9;
                inventory = createInventory(player);
                inventories[++index] = inventory;
            }
        }
    }

    private boolean isValid(Player player, BuildWorld buildWorld) {
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !buildWorld.isPrivate()) {
            if (player.hasPermission(buildWorld.getPermission()) || buildWorld.getPermission().equalsIgnoreCase("-")) {
                return Bukkit.getWorld(buildWorld.getName()) != null || (Bukkit.getWorld(buildWorld.getName()) == null && !buildWorld.isLoaded());
            }
        }
        return false;
    }

    public Integer getInvIndex(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!invIndex.containsKey(playerUUID)) {
            setInvIndex(player, 0);
        }
        return invIndex.get(playerUUID);
    }

    public void setInvIndex(Player player, int index) {
        invIndex.put(player.getUniqueId(), index);
    }

    public void incrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) + 1);
    }

    public void decrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) - 1);
    }
}
