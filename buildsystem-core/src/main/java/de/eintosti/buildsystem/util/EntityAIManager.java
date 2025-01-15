/*
 * Copyright (c) 2018-2025, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.util;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.internal.CraftBukkitVersion;
import java.lang.reflect.Method;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

public final class EntityAIManager {

    private static Class<?> nmsEntityClass;
    private static Class<?> nbtTagClass;

    private static Method getHandle;
    private static Method getNBTTag;
    private static Method c;
    private static Method setInt;
    private static Method f;

    private EntityAIManager() {
    }

    public static void setAIEnabled(LivingEntity entity, boolean enabled) {
        switch (entity.getType()) {
            case ARMOR_STAND:
            case ITEM_FRAME:
            case PAINTING:
            case PLAYER:
                return;
        }

        CraftBukkitVersion craftBukkitVersion = JavaPlugin.getPlugin(BuildSystem.class).getCraftBukkitVersion();

        switch (craftBukkitVersion) {
            case v1_8_R1:
            case v1_8_R2:
            case v1_8_R3:
                // Drop through
                break;
            default:
                entity.setAI(enabled);
                return;
        }

        try {
            String version = craftBukkitVersion.name();
            if (getHandle == null) {
                Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");
                getHandle = craftEntity.getDeclaredMethod("getHandle");
                getHandle.setAccessible(true);
            }

            Object nmsEntity = getHandle.invoke(entity);
            if (nmsEntityClass == null) {
                nmsEntityClass = Class.forName("net.minecraft.server." + version + ".Entity");
            }

            if (getNBTTag == null) {
                getNBTTag = nmsEntityClass.getDeclaredMethod("getNBTTag");
                getNBTTag.setAccessible(true);
            }

            if (nbtTagClass == null) {
                nbtTagClass = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
            }

            Object tag = getNBTTag.invoke(nmsEntity);
            if (tag == null) {
                tag = nbtTagClass.getDeclaredConstructor().newInstance();
            }

            if (c == null) {
                c = nmsEntityClass.getDeclaredMethod("c", nbtTagClass);
                c.setAccessible(true);
            }
            c.invoke(nmsEntity, tag);

            if (setInt == null) {
                setInt = nbtTagClass.getDeclaredMethod("setInt", String.class, Integer.TYPE);
                setInt.setAccessible(true);
            }

            int value = enabled ? 0 : 1;
            setInt.invoke(tag, "NoAI", value);

            if (f == null) {
                f = nmsEntityClass.getDeclaredMethod("f", nbtTagClass);
                f.setAccessible(true);
            }
            f.invoke(nmsEntity, tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}