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
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.LogoutLocation;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.config.Config.Settings.Archive;
import de.eintosti.buildsystem.player.LogoutLocationImpl;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.SpawnManager;
import io.papermc.lib.PaperLib;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerJoinListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerManager;
    private final SettingsManager settingsManager;
    private final SpawnManager spawnManager;
    private final WorldStorageImpl worldStorage;

    public PlayerJoinListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsManager();
        this.spawnManager = plugin.getSpawnManager();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerJoinMessage(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String message = Config.Messages.joinQuitMessages
                ? Messages.getString("player_join", player, Map.entry("%player%", player.getName()))
                : null;
        event.setJoinMessage(message);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUIDFetcher.cacheUser(player.getUniqueId(), player.getName());

        BuildPlayer buildPlayer = playerManager.getPlayerStorage().createBuildPlayer(player);
        manageHidePlayer(player, buildPlayer);
        manageSettings(player, buildPlayer.getSettings());
        teleportToCorrectLocation(player, buildPlayer);
        playerManager.giveNavigator(player);

        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld != null) {
            WorldData worldData = buildWorld.getData();
            if (!worldData.physics().get() && player.hasPermission("buildsystem.physics.message")) {
                Messages.sendMessage(player, "physics_deactivated_in_world", Map.entry("%world%", worldName));
            }

            if (Archive.vanish && worldData.status().get() == BuildWorldStatus.ARCHIVE) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false), false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        }

        if (player.hasPermission("buildsystem.updates")) {
            performUpdateCheck(player);
        }
    }

    /**
     * Teleports the player to the correct location.
     * <ul>
     *   <li>If the spawn exists and {@link SettingsImpl#isSpawnTeleport()} is enabled for the player, the player will be teleported to the spawn</li>
     *   <li>If the player has a {@link LogoutLocationImpl}, teleport to that location</li>
     *   <li>Otherwise, do nothing</li>
     * </ul>
     *
     * @param player      The player to teleport
     * @param buildPlayer The build-player for the given player
     */
    private void teleportToCorrectLocation(Player player, BuildPlayer buildPlayer) {
        if (buildPlayer.getSettings().isSpawnTeleport() && spawnManager.spawnExists()) {
            spawnManager.teleport(player);
            return;
        }

        LogoutLocation logoutLocation = buildPlayer.getLogoutLocation();
        if (logoutLocation == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(logoutLocation.worldName());
        if (buildWorld == null) {
            return;
        }

        int delay = buildWorld.isLoaded() ? 0 : 20;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location location = logoutLocation.location();
            if (location != null) {
                PaperLib.teleportAsync(player, location);
            }
        }, delay);
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player, BuildPlayer buildPlayer) {
        // Hide all players to player
        if (buildPlayer.getSettings().isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        }

        // Hide player from all players who have hidePlayers enabled
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!settingsManager.getSettings(pl).isHidePlayers()) {
                continue;
            }
            pl.hidePlayer(player);
        }
    }

    /**
     * Activates features for the player according to their {@link SettingsImpl}.
     *
     * @param player   The player to activate the features for
     * @param settings The player's settings
     */
    private void manageSettings(Player player, Settings settings) {
        if (settings.isNoClip()) {
            plugin.getNoClipManager().startNoClip(player);
        }

        if (settings.isScoreboard()) {
            settingsManager.startScoreboard(player);
            plugin.getPlayerService().forceUpdateSidebar(player);
        }

        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }
    }

    private void performUpdateCheck(Player player) {
        if (!Config.Settings.updateChecker) {
            return;
        }

        UpdateChecker.init(plugin, BuildSystemPlugin.SPIGOT_ID)
                .requestUpdateCheck()
                .whenComplete((result, e) -> {
                    if (result.requiresUpdate()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        Messages.getStringList("update_available", player).forEach(line ->
                                stringBuilder.append(line
                                                .replace("%new_version%", result.getNewestVersion())
                                                .replace("%current_version%", plugin.getDescription().getVersion()))
                                        .append("\n"));
                        player.sendMessage(stringBuilder.toString());
                    }
                });
    }
}