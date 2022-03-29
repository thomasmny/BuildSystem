/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.manager;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.util.external.ItemSkulls;
import com.google.common.collect.Sets;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author einTosti
 */
public class ArmorStandManager {

    private static final float RADIUS = 2.2f;
    private static final float SPREAD = 90.0f;

    private final Map<UUID, ArmorStand[]> armorStands;

    public ArmorStandManager() {
        this.armorStands = new HashMap<>();
    }

    private Location calculatePosition(Player player, float angle) {
        float centerX = (float) player.getLocation().getX();
        float centerZ = (float) player.getLocation().getZ();
        float yaw = player.getLocation().getYaw() + 180 + angle;
        float xPos = RADIUS * (float) Math.cos(Math.toRadians(yaw - 90)) + centerX;
        float zPos = RADIUS * (float) Math.sin(Math.toRadians(yaw - 90)) + centerZ;

        Location location = new Location(player.getWorld(), xPos, player.getLocation().getY(), zPos);
        location.setYaw(yaw);
        return location;
    }

    @SuppressWarnings("deprecation")
    private ArmorStand spawnArmorStand(Player player, Location location, String customName, boolean customSkull, String skullUrl) {
        location.setY(location.getY() - 0.1);

        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setCustomName(player.getName() + " × " + customName);
        armorStand.setCustomNameVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);

        ItemStack skull;
        if (customSkull) {
            skull = ItemSkulls.getSkull(skullUrl);
        } else {
            skull = XMaterial.PLAYER_HEAD.parseItem();
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwner(player.getName());
            skull.setItemMeta(skullMeta);
        }
        armorStand.setHelmet(skull);

        return armorStand;
    }

    private ArmorStand spawnWorldNavigator(Player player) {
        Location navigatorLocation = calculatePosition(player, SPREAD / 2 * -1);
        return spawnArmorStand(player, navigatorLocation, "§aWorld Navigator", true, "https://textures.minecraft.net/texture/d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622");
    }

    private ArmorStand spawnWorldArchive(Player player) {
        Location archiveLocation = calculatePosition(player, 0);
        return spawnArmorStand(player, archiveLocation, "§6World Archive", true, "https://textures.minecraft.net/texture/7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8");
    }

    private ArmorStand spawnPrivateWorlds(Player player) {
        Location privateLocation = calculatePosition(player, SPREAD / 2);
        return spawnArmorStand(player, privateLocation, "§bPrivate Worlds", false, player.getName());
    }

    public void spawnArmorStands(Player player) {
        ArmorStand worldNavigator = spawnWorldNavigator(player);
        ArmorStand worldArchive = spawnWorldArchive(player);
        ArmorStand privateWorlds = spawnPrivateWorlds(player);

        this.armorStands.put(player.getUniqueId(), new ArmorStand[]{worldNavigator, worldArchive, privateWorlds});
    }

    public void removeArmorStands(Player player) {
        ArmorStand[] armorStands = this.armorStands.get(player.getUniqueId());
        if (armorStands == null) {
            return;
        }

        String playerName = player.getName();
        Set<String> names = Sets.newHashSet(
                "§aWorld Navigator",
                "§6World Archive",
                "§bPrivate Worlds"
        );

        for (ArmorStand armorStand : armorStands) {
            String customName = armorStand.getCustomName();
            if (customName == null) {
                continue;
            }

            for (String name : names) {
                if (customName.equals(playerName + " × " + name)) {
                    armorStand.remove();
                    break;
                }
            }
        }
    }
}