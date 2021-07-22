package de.eintosti.buildsystem.version;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * @author einTosti
 */
public class SkullCache_1_17_R1 implements SkullCache {
    private final Map<String, net.minecraft.world.item.ItemStack> skullCache;

    public SkullCache_1_17_R1() {
        this.skullCache = new HashMap<>();
    }

    @SuppressWarnings("deprecation")
    private ItemStack getPlayerSkull(String name) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setOwner(name);
        skull.setItemMeta(skullMeta);

        return skull;
    }

    @Override
    public void cacheSkull(String name) {
        skullCache.put(name, CraftItemStack.asNMSCopy(getPlayerSkull(name)));
    }

    @Override
    public ItemStack getCachedSkull(String name) {
        net.minecraft.world.item.ItemStack cachedSkull = this.skullCache.get(name);

        if (cachedSkull != null) {
            return CraftItemStack.asBukkitCopy(cachedSkull);
        } else {
            ItemStack skull = getPlayerSkull(name);
            skullCache.put(name, CraftItemStack.asNMSCopy(skull));
            return skull;
        }
    }
}
