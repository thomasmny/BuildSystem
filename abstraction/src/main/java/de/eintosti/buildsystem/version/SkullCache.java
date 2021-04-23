package de.eintosti.buildsystem.version;

import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public interface SkullCache {

    void cacheSkull(String name);

    ItemStack getCachedSkull(String name);
}
