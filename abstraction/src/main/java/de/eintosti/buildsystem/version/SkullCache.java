package de.eintosti.buildsystem.version;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public interface SkullCache {

    void cacheSkull(Player player);

    ItemStack getCachedSkull(String name);
}
