/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.manager.*;
import com.eintosti.buildsystem.object.player.BuildPlayer;
import com.eintosti.buildsystem.object.player.LogoutLocation;
import com.eintosti.buildsystem.object.settings.Settings;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.data.WorldStatus;
import com.eintosti.buildsystem.util.external.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author einTosti
 */
public class PlayerJoinListener implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final InventoryManager inventoryManager;
    private final PlayerManager playerManager;
    private final SettingsManager settingsManager;
    private final SpawnManager spawnManager;
    private final WorldManager worldManager;

    public PlayerJoinListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.inventoryManager = plugin.getInventoryManager();
        this.playerManager = plugin.getPlayerManager();
        this.settingsManager = plugin.getSettingsManager();
        this.spawnManager = plugin.getSpawnManager();
        this.worldManager = plugin.getWorldManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerJoinMessage(PlayerJoinEvent event) {
        boolean isJoinMessage = plugin.getConfigValues().isJoinQuitMessages();
        String message = isJoinMessage ? plugin.getString("player_join").replace("%player%", event.getPlayer().getName()) : null;
        event.setJoinMessage(message);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getSkullCache().cacheSkull(player.getName());

        BuildPlayer buildPlayer = playerManager.createBuildPlayer(player);
        manageHidePlayer(player, buildPlayer);

        Settings settings = buildPlayer.getSettings();
        if (settings.isNoClip()) {
            plugin.getNoClipManager().startNoClip(player);
        }
        if (settings.isScoreboard()) {
            settingsManager.startScoreboard(player);
            plugin.getPlayerManager().forceUpdateSidebar(player);
        }
        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }
        addJoinItem(player);

        if (settings.isSpawnTeleport() && spawnManager.spawnExists()) {
            spawnManager.teleport(player);
        } else {
            LogoutLocation logoutLocation = buildPlayer.getLogoutLocation();
            if (logoutLocation != null) {
                player.teleport(logoutLocation.getLocation());
            }
        }

        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld != null) {
            if (!buildWorld.isPhysics() && player.hasPermission("buildsystem.physics.message")) {
                player.sendMessage(plugin.getString("physics_deactivated_in_world").replace("%world%", buildWorld.getName()));
            }

            if (configValues.isArchiveVanish() && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        }

        if (player.hasPermission("buildsystem.updates")) {
            performUpdateCheck(player);
        }
    }

    private void addJoinItem(Player player) {
        if (!configValues.isGiveNavigatorOnJoin()) {
            return;
        }

        if (!player.hasPermission("buildsystem.navigator.item")) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        if (inventoryManager.inventoryContainsNavigator(playerInventory)) {
            return;
        }

        ItemStack itemStack = inventoryManager.getItemStack(configValues.getNavigatorItem(), plugin.getString("navigator_item"));
        ItemStack slot8 = playerInventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.parseMaterial()) {
            playerInventory.setItem(8, itemStack);
        } else {
            playerInventory.addItem(itemStack);
        }
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player, BuildPlayer buildPlayer) {
        if (buildPlayer.getSettings().isHidePlayers()) { // Hide all players to player
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        }

        for (Player pl : Bukkit.getOnlinePlayers()) { // Hide player to all players who have hidePlayers enabled
            if (!settingsManager.getSettings(pl).isHidePlayers()) {
                continue;
            }
            pl.hidePlayer(player);
        }
    }

    private void performUpdateCheck(Player player) {
        if (!configValues.isUpdateChecker()) {
            return;
        }

        UpdateChecker.init(plugin, BuildSystem.SPIGOT_ID)
                .requestUpdateCheck()
                .whenComplete((result, e) -> {
                    if (result.requiresUpdate()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        plugin.getStringList("update_available").forEach(line ->
                                stringBuilder.append(line
                                                .replace("%new_version%", result.getNewestVersion())
                                                .replace("%current_version%", plugin.getDescription().getVersion()))
                                        .append("\n"));
                        player.sendMessage(stringBuilder.toString());
                    }
                });
    }
}