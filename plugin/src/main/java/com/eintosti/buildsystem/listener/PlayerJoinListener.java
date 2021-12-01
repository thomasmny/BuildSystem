/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.manager.SpawnManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.settings.Settings;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.WorldStatus;
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
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;
    private final SpawnManager spawnManager;
    private final WorldManager worldManager;

    public PlayerJoinListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
        this.spawnManager = plugin.getSpawnManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerJoinMessage(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinMessage = plugin.isJoinQuitMessages() ? plugin.getString("player_join").replace("%player%", player.getName()) : null;
        event.setJoinMessage(joinMessage);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        settingsManager.createSettings(player);
        plugin.getSkullCache().cacheSkull(player.getName());

        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld != null) {
            if (!buildWorld.isPhysics() && player.hasPermission("buildsystem.physics.message")) {
                player.sendMessage(plugin.getString("physics_deactivated_in_world").replace("%world%", buildWorld.getName()));
            }

            if (plugin.isArchiveVanish() && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        }

        Settings settings = settingsManager.getSettings(player);
        if (settings.isNoClip()) {
            plugin.getNoClipManager().startNoClip(player);
        }
        if (settings.isScoreboard()) {
            settingsManager.startScoreboard(player);
            plugin.forceUpdateSidebar(player);
        }
        if (settings.isSpawnTeleport()) {
            spawnManager.teleport(player);
        }
        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }

        manageHidePlayer(player);
        addJoinItem(player);

        if (plugin.isUpdateChecker()) {
            if (player.hasPermission("buildsystem.updates")) {
                performUpdateCheck(player);
            }
        }
    }

    private void addJoinItem(Player player) {
        if (!plugin.isGiveNavigatorOnJoin()) {
            return;
        }

        if (!player.hasPermission("buildsystem.gui")) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        if (inventoryManager.inventoryContainsNavigator(playerInventory)) {
            return;
        }

        ItemStack itemStack = inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item"));
        ItemStack slot8 = playerInventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.parseMaterial()) {
            playerInventory.setItem(8, itemStack);
        } else {
            playerInventory.addItem(itemStack);
        }
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player) {
        if (settingsManager.getSettings(player).isHidePlayers()) { // Hide all players to player
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        }

        for (Player pl : Bukkit.getOnlinePlayers()) { // Hide player to all players who have hidePlayers enabled
            if (!settingsManager.getSettings(pl).isHidePlayers()) continue;
            pl.hidePlayer(player);
        }
    }

    private void performUpdateCheck(Player player) {
        UpdateChecker.init(plugin, BuildSystem.PLUGIN_ID)
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
