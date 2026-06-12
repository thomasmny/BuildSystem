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
package de.eintosti.buildsystem.navigator;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import java.util.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NavigatorService {

    private static final float RADIUS = 2.2f;
    private static final float SPREAD = 90.0f;

    private final BuildSystemPlugin plugin;
    private final NamespacedKey ownerKey;
    private final NamespacedKey categoryKey;

    private final Set<Player> openNavigator;
    private final Map<UUID, ArmorStand[]> armorStands;

    public NavigatorService(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.categoryKey = new NamespacedKey(plugin, "category");
        this.openNavigator = new HashSet<>();
        this.armorStands = new HashMap<>();
        initEntityChecker();
    }

    // -- ArmorStand helpers -----------------------------------------------

    @Nullable public NavigatorCategory matchNavigatorCategory(ArmorStand armorStand) {
        String categoryName = armorStand.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        return categoryName != null ? NavigatorCategory.valueOf(categoryName) : null;
    }

    @Nullable public UUID getOwner(ArmorStand armorStand) {
        String ownerUUID = armorStand.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return ownerUUID != null ? UUID.fromString(ownerUUID) : null;
    }

    public void spawnArmorStands(Player player) {
        ArmorStand worldNavigator = spawnWorldNavigator(player);
        ArmorStand worldArchive = spawnWorldArchive(player);
        ArmorStand privateWorlds = spawnPrivateWorlds(player);
        this.armorStands.put(player.getUniqueId(), new ArmorStand[] {worldNavigator, worldArchive, privateWorlds});
    }

    public void removeArmorStands(Player player) {
        ArmorStand[] stands = this.armorStands.remove(player.getUniqueId());
        if (stands == null) {
            return;
        }
        for (ArmorStand armorStand : stands) {
            armorStand.remove();
        }
    }

    private ArmorStand spawnWorldNavigator(Player player) {
        Location navigatorLocation = calculatePosition(player, SPREAD / 2 * -1);
        return spawnArmorStand(
                player,
                navigatorLocation,
                NavigatorCategory.PUBLIC,
                true,
                "d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622");
    }

    private ArmorStand spawnWorldArchive(Player player) {
        Location archiveLocation = calculatePosition(player, 0);
        return spawnArmorStand(
                player,
                archiveLocation,
                NavigatorCategory.ARCHIVE,
                true,
                "7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8");
    }

    private ArmorStand spawnPrivateWorlds(Player player) {
        Location privateLocation = calculatePosition(player, SPREAD / 2);
        return spawnArmorStand(player, privateLocation, NavigatorCategory.PRIVATE, false, player.getName());
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

    private ArmorStand spawnArmorStand(
            Player player, Location location, NavigatorCategory category, boolean customSkull, String skullUrl) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand
                .getEquipment()
                .setHelmet(XSkull.createItem()
                        .profile(Profileable.detect(customSkull ? skullUrl : player.getName()))
                        .apply());

        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(categoryKey, PersistentDataType.STRING, category.name());

        return armorStand;
    }

    // -- Navigator open/close logic ----------------------------------------

    public Set<Player> getOpenNavigator() {
        return openNavigator;
    }

    public void giveNavigator(Player player) {
        if (!plugin.getConfigService().current().settings().navigator().giveItemOnJoin()
                || !player.hasPermission("buildsystem.navigator.item")
                || plugin.getMenuItems().hasNavigator(player)) {
            return;
        }

        ItemStack navigatorItem = plugin.getMenuItems().createNavigatorItem(player);
        PlayerInventory playerInventory = player.getInventory();
        ItemStack slot8 = playerInventory.getItem(8);

        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            playerInventory.setItem(8, navigatorItem);
        } else {
            playerInventory.addItem(navigatorItem);
        }
    }

    public void closeNewNavigator(Player player) {
        if (!openNavigator.contains(player)) {
            return;
        }

        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(plugin.getPlayerService().getPlayerStorage().getBuildPlayer(player));
        buildPlayer.setLastLookedAt(null);
        removeArmorStands(player);

        XSound.ENTITY_ITEM_BREAK.play(player);
        displayActionBarMessage(player, "");
        plugin.getMenuItems()
                .replaceItem(
                        player,
                        plugin.getMessages().getString("barrier_item", player),
                        XMaterial.BARRIER,
                        plugin.getMenuItems().createNavigatorItem(player));

        CachedValues cachedValues = buildPlayer.getCachedValues();
        cachedValues.resetWalkSpeedIfPresent(player);
        cachedValues.resetFlySpeedIfPresent(player);
        player.removePotionEffect(XPotion.JUMP_BOOST.get());
        player.removePotionEffect(XPotion.BLINDNESS.get());

        openNavigator.remove(player);
    }

    // -- Entity-checker tick -----------------------------------------------

    private void initEntityChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForArmorStandNavigator, 0L, 2L);
    }

    private void checkForArmorStandNavigator() {
        for (Player player : openNavigator) {
            if (!(getTargetEntity(player) instanceof ArmorStand armorStand)) {
                continue;
            }

            BuildPlayerImpl buildPlayer = BuildPlayerImpl.of(
                    plugin.getPlayerService().getPlayerStorage().getBuildPlayer(player));
            if (isLookingAtArmorStandHead(player, armorStand)) {
                NavigatorCategory category = matchNavigatorCategory(armorStand);
                sendTypeInfo(player, category);
            } else {
                sendTypeInfo(player, null);
            }
        }
    }

    private boolean isLookingAtArmorStandHead(Player player, ArmorStand armorStand) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        Location standEyeLocation = armorStand.getEyeLocation().clone();
        double boxHalfSize = 0.3;

        double minX = standEyeLocation.getX() - boxHalfSize;
        double maxX = standEyeLocation.getX() + boxHalfSize;
        double minY = standEyeLocation.getY() - boxHalfSize;
        double maxY = standEyeLocation.getY() + boxHalfSize;
        double minZ = standEyeLocation.getZ() - boxHalfSize;
        double maxZ = standEyeLocation.getZ() + boxHalfSize;

        for (double distance = 0; distance <= 3; distance += 0.05) {
            Vector point = eyeLocation.toVector().add(direction.clone().multiply(distance));

            if (point.getX() >= minX
                    && point.getX() <= maxX
                    && point.getY() >= minY
                    && point.getY() <= maxY
                    && point.getZ() >= minZ
                    && point.getZ() <= maxZ) {
                return true;
            }
        }

        return false;
    }

    @Nullable private Entity getTargetEntity(Entity entity) {
        return getTarget(entity, entity.getNearbyEntities(3, 3, 3));
    }

    @Nullable @Contract("null, _ -> null")
    private <T extends Entity> T getTarget(@Nullable Entity entity, Iterable<T> entities) {
        if (entity == null) {
            return null;
        }

        T target = null;
        final Location entityLocation = entity.getLocation();
        final double threshold = 0.5;

        for (T other : entities) {
            final Location otherLocation = other.getLocation();
            final Vector vector = otherLocation.toVector().subtract(entityLocation.toVector());

            if (entityLocation.getDirection().normalize().crossProduct(vector).lengthSquared() < threshold
                    && vector.normalize().dot(entityLocation.getDirection().normalize()) >= 0) {
                if (target == null
                        || target.getLocation().distanceSquared(entityLocation)
                                > otherLocation.distanceSquared(entityLocation)) {
                    target = other;
                }
            }
        }

        return target;
    }

    private void sendTypeInfo(Player player, @Nullable NavigatorCategory category) {
        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(plugin.getPlayerService().getPlayerStorage().getBuildPlayer(player));
        if (category == null) {
            buildPlayer.setLastLookedAt(null);
            displayActionBarMessage(player, "§0");
            return;
        }

        NavigatorCategory lastLookedAt = buildPlayer.getLastLookedAt();
        if (lastLookedAt == null || lastLookedAt != category) {
            buildPlayer.setLastLookedAt(category);
            XSound.ENTITY_CHICKEN_EGG.play(player);
        }

        String message =
                switch (category) {
                    case PUBLIC -> "new_navigator_world_navigator";
                    case ARCHIVE -> "new_navigator_world_archive";
                    case PRIVATE -> "new_navigator_private_worlds";
                };
        displayActionBarMessage(player, plugin.getMessages().getString(message, player));
    }

    private void displayActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
