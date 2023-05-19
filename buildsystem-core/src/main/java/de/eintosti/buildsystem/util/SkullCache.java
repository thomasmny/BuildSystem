/*
 * Copyright (c) 2023, Thomas Meaney
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

public class SkullCache {

    private final Map<String, Object> skullCache;

    private Class<?> craftItemStackClass;

    public SkullCache(String version) {
        try {
            this.craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        this.skullCache = new HashMap<>();
    }

    @SuppressWarnings("deprecation")
    private ItemStack getSkull(String name) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setOwner(name);
        skull.setItemMeta(skullMeta);

        return skull;
    }

    private Object getNmsSkull(String name) throws Exception {
        ItemStack skull = getSkull(name);
        Method getNMSItem = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
        return getNMSItem.invoke(null, skull);
    }

    public void cacheSkull(String name) {
        try {
            skullCache.put(name, getNmsSkull(name));
            Bukkit.getLogger().info("Cached skull for: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ItemStack getCachedSkull(String name) {
        try {
            this.skullCache.putIfAbsent(name, getNmsSkull(name));
            Object cachedSkull = this.skullCache.get(name);
            return (ItemStack) craftItemStackClass.getMethod("asBukkitCopy", cachedSkull.getClass()).invoke(null, cachedSkull);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return XMaterial.PLAYER_HEAD.parseItem();
    }
}