package de.eintosti.buildsystem.version;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * @author einTosti
 */
public class SkullCache_1_11_R1 implements SkullCache {
    private final Map<String, net.minecraft.server.v1_11_R1.ItemStack> skullCache;

    public SkullCache_1_11_R1() {
        this.skullCache = new HashMap<>();
    }

    private ItemStack getPlayerSkull(String name) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setOwner(name);
        skull.setItemMeta(skullMeta);

        return skull;
    }

    @Override
    public void cacheSkull(Player player) {
        skullCache.put(player.getName(), CraftItemStack.asNMSCopy(getPlayerSkull(player.getName())));
    }

    @Override
    public ItemStack getCachedSkull(String name) {
        net.minecraft.server.v1_11_R1.ItemStack cachedSkull = this.skullCache.get(name);
        return cachedSkull != null ? CraftItemStack.asBukkitCopy(cachedSkull) : getPlayerSkull(name);
    }
}
