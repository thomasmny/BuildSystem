/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.manager;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.util.ConfigValues;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author einTosti
 */
public class PlayerManager {

    private static final double MIN_HEIGHT = -0.16453003708696978;
    private static final double MAX_HEIGHT = 0.16481381407766063;
    private static final float DEFAULT_SPEED = 0.2f;

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final Map<UUID, Location> previousLocation;
    private final Map<UUID, BuildWorld> selectedWorld;
    private final Map<UUID, String> lastLookedAt;
    private final Map<UUID, GameMode> playerGamemode;
    private final Map<UUID, Float> playerWalkSpeed;
    private final Map<UUID, Float> playerFlySpeed;

    private final Set<Player> openNavigator;
    private final Set<UUID> buildPlayers;

    public PlayerManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.previousLocation = new HashMap<>();
        this.selectedWorld = new HashMap<>();
        this.lastLookedAt = new HashMap<>();
        this.playerGamemode = new HashMap<>();
        this.playerWalkSpeed = new HashMap<>();
        this.playerFlySpeed = new HashMap<>();

        this.openNavigator = new HashSet<>();
        this.buildPlayers = new HashSet<>();

        initEntityChecker();
    }

    public Map<UUID, BuildWorld> getSelectedWorld() {
        return selectedWorld;
    }

    @Nullable
    public String getSelectedWorldName(Player player) {
        BuildWorld selectedWorld = getSelectedWorld().get(player.getUniqueId());
        if (selectedWorld == null) {
            return null;
        }

        String selectedWorldName = selectedWorld.getName();
        if (selectedWorldName.length() > 17) {
            selectedWorldName = selectedWorldName.substring(0, 14) + "...";
        }
        return selectedWorldName;
    }

    public Map<UUID, Location> getPreviousLocation() {
        return previousLocation;
    }

    public Map<UUID, GameMode> getPlayerGamemode() {
        return playerGamemode;
    }

    public Map<UUID, Float> getPlayerWalkSpeed() {
        return playerWalkSpeed;
    }

    public Map<UUID, Float> getPlayerFlySpeed() {
        return playerFlySpeed;
    }

    public Set<Player> getOpenNavigator() {
        return openNavigator;
    }

    public Set<UUID> getBuildPlayers() {
        return buildPlayers;
    }

    public void forceUpdateSidebar(BuildWorld buildWorld) {
        if (!configValues.isScoreboard()) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        bukkitWorld.getPlayers().forEach(this::forceUpdateSidebar);
    }

    public void forceUpdateSidebar(Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        if (!configValues.isScoreboard() || !settingsManager.getSettings(player).isScoreboard()) {
            return;
        }
        settingsManager.updateScoreboard(player);
    }

    public void closeNavigator(Player player) {
        if (!openNavigator.contains(player)) {
            return;
        }

        lastLookedAt.remove(player.getUniqueId());
        plugin.getArmorStandManager().removeArmorStands(player);

        XSound.ENTITY_ITEM_BREAK.play(player);
        ActionBar.clearActionBar(player);
        replaceBarrier(player);

        UUID playerUuid = player.getUniqueId();
        player.setWalkSpeed(playerWalkSpeed.getOrDefault(playerUuid, DEFAULT_SPEED));
        player.setFlySpeed(playerFlySpeed.getOrDefault(playerUuid, DEFAULT_SPEED));
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        playerWalkSpeed.remove(playerUuid);
        playerFlySpeed.remove(playerUuid);
        openNavigator.remove(player);
    }

    private void replaceBarrier(Player player) {
        if (!player.hasPermission("buildsystem.gui")) {
            return;
        }

        InventoryManager inventoryManager = plugin.getInventoryManager();
        String findItemName = plugin.getString("barrier_item");
        ItemStack replaceItem = inventoryManager.getItemStack(plugin.getConfigValues().getNavigatorItem(), plugin.getString("navigator_item"));

        inventoryManager.replaceItem(player, findItemName, XMaterial.BARRIER, replaceItem);
    }

    private void initEntityChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForEntity, 0L, 1L);
    }

    private void checkForEntity() {
        List<UUID> toRemove = new ArrayList<>();

        for (Player player : openNavigator) {
            if (getEntityName(player).isEmpty()) {
                continue;
            }

            double lookedPosition = player.getEyeLocation().getDirection().getY();
            if (lookedPosition >= MIN_HEIGHT && lookedPosition <= MAX_HEIGHT) {
                String invType = getEntityName(player).replace(player.getName() + " × ", "");
                String lastLookedAt = this.lastLookedAt.get(player.getUniqueId());

                if (lastLookedAt == null || !lastLookedAt.equals(invType)) {
                    this.lastLookedAt.put(player.getUniqueId(), invType);
                    sendTypeInfo(player, invType);
                }
            } else {
                ActionBar.clearActionBar(player);
                toRemove.add(player.getUniqueId());
            }
        }

        toRemove.forEach(lastLookedAt::remove);
    }

    @Nullable
    private <T extends Entity> T getTarget(Entity entity, Iterable<T> entities) {
        if (entity == null) {
            return null;
        }

        T target = null;
        final Location entityLocation = entity.getLocation();
        final double threshold = 0.5;

        for (T other : entities) {
            final Location otherLocation = other.getLocation();
            final Vector vector = otherLocation.toVector().subtract(entityLocation.toVector());

            if (entityLocation.getDirection().normalize().crossProduct(vector).lengthSquared() < threshold && vector.normalize().dot(entityLocation.getDirection().normalize()) >= 0) {
                if (target == null || target.getLocation().distanceSquared(entityLocation) > otherLocation.distanceSquared(entityLocation)) {
                    target = other;
                }
            }
        }

        return target;
    }

    @Nullable
    private Entity getTargetEntity(Entity entity) {
        return getTarget(entity, entity.getNearbyEntities(3, 3, 3));
    }

    @NotNull
    private String getEntityName(Player player) {
        Entity targetEntity = getTargetEntity(player);
        if (targetEntity == null || targetEntity.getType() != EntityType.ARMOR_STAND) {
            return "";
        }

        Entity entity = getTargetEntity(player);
        if (entity == null || entity.getCustomName() == null) {
            return "";
        }

        return entity.getCustomName();
    }

    private void sendTypeInfo(Player player, String invType) {
        String message;
        switch (invType) {
            case "§aWorld Navigator":
                message = "new_navigator_world_navigator";
                break;
            case "§6World Archive":
                message = "new_navigator_world_archive";
                break;
            case "§bPrivate Worlds":
                message = "new_navigator_private_worlds";
                break;
            default:
                ActionBar.clearActionBar(player);
                return;
        }

        ActionBar.sendActionBar(player, plugin.getString(message));
        XSound.ENTITY_CHICKEN_EGG.play(player);
    }
}