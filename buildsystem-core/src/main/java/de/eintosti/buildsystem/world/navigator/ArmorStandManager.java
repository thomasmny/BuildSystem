/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.world.navigator;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ArmorStandManager {

    private static final JavaPlugin PLUGIN = JavaPlugin.getProvidingPlugin(ArmorStandManager.class);
    private static final NamespacedKey OWNER_KEY = new NamespacedKey(PLUGIN, "owner");
    private static final NamespacedKey CATEGORY_KEY = new NamespacedKey(PLUGIN, "category");

    private static final float RADIUS = 2.2f;
    private static final float SPREAD = 90.0f;

    private final Map<UUID, ArmorStand[]> armorStands;

    public ArmorStandManager() {
        this.armorStands = new HashMap<>();
    }

    @Nullable
    public static NavigatorCategory matchNavigatorCategory(ArmorStand armorStand) {
        String categoryName = armorStand.getPersistentDataContainer().get(CATEGORY_KEY, PersistentDataType.STRING);
        return categoryName != null ? NavigatorCategory.valueOf(categoryName) : null;
    }

    @Nullable
    public static UUID getOwner(ArmorStand armorStand) {
        String ownerUUID = armorStand.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
        return ownerUUID != null ? UUID.fromString(ownerUUID) : null;
    }

    private Location calculatePosition(Player player, float angle) {
        Location playerLocation = player.getLocation();
        float centerX = (float) playerLocation.getX();
        float centerZ = (float) playerLocation.getZ();
        float yaw = playerLocation.getYaw() + 180 + angle;
        float xPos = RADIUS * (float) Math.cos(Math.toRadians(yaw - 90)) + centerX;
        double yPos = playerLocation.getY() - 0.1;
        float zPos = RADIUS * (float) Math.sin(Math.toRadians(yaw - 90)) + centerZ;

        Location location = new Location(player.getWorld(), xPos, yPos, zPos);
        location.setYaw(yaw);
        return location;
    }

    private ArmorStand spawnArmorStand(Player player, Location location, NavigatorCategory category, boolean customSkull, String skullUrl) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.getEquipment().setHelmet(
                XSkull.createItem()
                        .profile(Profileable.detect(customSkull ? skullUrl : player.getName()))
                        .apply()
        );

        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        pdc.set(OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(CATEGORY_KEY, PersistentDataType.STRING, category.name());

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
        return spawnArmorStand(player, navigatorLocation, NavigatorCategory.PUBLIC, true, "d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622");
    }

    private ArmorStand spawnWorldArchive(Player player) {
        Location archiveLocation = calculatePosition(player, 0);
        return spawnArmorStand(player, archiveLocation, NavigatorCategory.ARCHIVE, true, "7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8");
    }

    private ArmorStand spawnPrivateWorlds(Player player) {
        Location privateLocation = calculatePosition(player, SPREAD / 2);
        return spawnArmorStand(player, privateLocation, NavigatorCategory.PRIVATE, false, player.getName());
    }

    public void removeArmorStands(Player player) {
        ArmorStand[] armorStands = this.armorStands.remove(player.getUniqueId());
        if (armorStands == null) {
            return;
        }

        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
    }
}