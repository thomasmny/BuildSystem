package de.eintosti.buildsystem.version;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * @author einTosti
 */
public interface GameRules {
    Inventory getInventory(Player player, World worldName);

    void addGameRules(World worldName);

    void toggleGameRule(InventoryClickEvent event, World world);

    void incrementInv(Player player);

    void decrementInv(Player player);

    int getInvIndex(UUID uuid);

    int getNumGameRules();

    int[] getSlots();
}
