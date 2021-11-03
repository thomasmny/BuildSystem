/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class SkullCache {
    private Class<?> craftItemStackClass;

    private final Map<String, Object> skullCache;

    public SkullCache() {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        try {
            this.craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        this.skullCache = new HashMap<>();
    }

    @SuppressWarnings("deprecation")
    private ItemStack getPlayerSkull(String name) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setOwner(name);
        skull.setItemMeta(skullMeta);

        return skull;
    }

    private Object[] getSkullAndNmsObject(String name) throws Exception {
        ItemStack skull = getPlayerSkull(name);
        Method getNMSItem = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        Object nmsItem = getNMSItem.invoke(null, skull);

        return new Object[]{skull, nmsItem};
    }

    public void cacheSkull(String name) {
        try {
            Bukkit.getLogger().log(Level.INFO, "Cached Skull for: " + name);
            skullCache.put(name, getSkullAndNmsObject(name)[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ItemStack getCachedSkull(String name) {
        try {
            Object cachedSkull = this.skullCache.get(name);

            if (cachedSkull != null) {
                return (ItemStack) craftItemStackClass.getMethod("asBukkitCopy", cachedSkull.getClass()).invoke(null, cachedSkull);
            } else {
                Object[] skullAndNmsObject = getSkullAndNmsObject(name);
                ItemStack skull = (ItemStack) skullAndNmsObject[0];
                Object nmsSkullObject = skullAndNmsObject[1];

                this.skullCache.put(name, nmsSkullObject);
                return skull;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return XMaterial.PLAYER_HEAD.parseItem();
    }
}
