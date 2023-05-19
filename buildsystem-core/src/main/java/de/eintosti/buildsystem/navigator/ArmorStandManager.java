/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.navigator.settings.NavigatorInventoryType;
import de.eintosti.buildsystem.util.ItemSkulls;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorStandManager {

    private static final float RADIUS = 2.2f;
    private static final float SPREAD = 90.0f;

    private final Map<UUID, ArmorStand[]> armorStands;

    public ArmorStandManager() {
        this.armorStands = new HashMap<>();
    }

    private Location calculatePosition(Player player, float angle) {
        Location playerLocation = player.getLocation();
        float centerX = (float) playerLocation.getX();
        float centerZ = (float) playerLocation.getZ();
        float yaw = playerLocation.getYaw() + 180 + angle;
        float xPos = RADIUS * (float) Math.cos(Math.toRadians(yaw - 90)) + centerX;
        float zPos = RADIUS * (float) Math.sin(Math.toRadians(yaw - 90)) + centerZ;

        Location location = new Location(player.getWorld(), xPos, playerLocation.getY(), zPos);
        location.setYaw(yaw);
        return location;
    }

    @SuppressWarnings("deprecation")
    private ArmorStand spawnArmorStand(Player player, Location location, NavigatorInventoryType inventoryType, boolean customSkull, String skullUrl) {
        location.setY(location.getY() - 0.1);

        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setCustomName(player.getName() + " × " + inventoryType.getArmorStandName());
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

    public void spawnArmorStands(Player player) {
        ArmorStand worldNavigator = spawnWorldNavigator(player);
        ArmorStand worldArchive = spawnWorldArchive(player);
        ArmorStand privateWorlds = spawnPrivateWorlds(player);

        this.armorStands.put(player.getUniqueId(), new ArmorStand[]{worldNavigator, worldArchive, privateWorlds});
    }

    private ArmorStand spawnWorldNavigator(Player player) {
        Location navigatorLocation = calculatePosition(player, SPREAD / 2 * -1);
        return spawnArmorStand(player, navigatorLocation, NavigatorInventoryType.NAVIGATOR, true, "d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622");
    }

    private ArmorStand spawnWorldArchive(Player player) {
        Location archiveLocation = calculatePosition(player, 0);
        return spawnArmorStand(player, archiveLocation, NavigatorInventoryType.ARCHIVE, true, "7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8");
    }

    private ArmorStand spawnPrivateWorlds(Player player) {
        Location privateLocation = calculatePosition(player, SPREAD / 2);
        return spawnArmorStand(player, privateLocation, NavigatorInventoryType.PRIVATE, false, player.getName());
    }

    public void removeArmorStands(Player player) {
        ArmorStand[] armorStands = this.armorStands.get(player.getUniqueId());
        if (armorStands == null) {
            return;
        }

        String playerName = player.getName();
        for (ArmorStand armorStand : armorStands) {
            String customName = armorStand.getCustomName();
            if (customName == null) {
                continue;
            }

            for (NavigatorInventoryType inventoryType : NavigatorInventoryType.values()) {
                if (customName.equals(playerName + " × " + inventoryType.getArmorStandName())) {
                    armorStand.remove();
                    break;
                }
            }
        }
    }
}