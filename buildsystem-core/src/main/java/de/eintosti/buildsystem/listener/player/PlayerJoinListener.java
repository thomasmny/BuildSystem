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
package de.eintosti.buildsystem.listener.player;

import com.cryptomorin.xseries.XPotion;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.spawn.SpawnService;
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
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerJoinListener implements Listener {

    private final PlayerService playerManager;
    private final SettingsService settingsManager;
    private final NavigatorService navigatorService;
    private final SpawnService spawnService;
    private final WorldStorage worldStorage;
    private final PlayerLookupService playerLookupService;
    private final NoClipService noClipService;
    private final ConfigService configService;
    private final Messages messages;
    private final UpdateChecker updateChecker;
    private final TaskScheduler scheduler;

    public PlayerJoinListener(
            PlayerService playerManager,
            SettingsService settingsManager,
            NavigatorService navigatorService,
            SpawnService spawnService,
            WorldStorage worldStorage,
            PlayerLookupService playerLookupService,
            NoClipService noClipService,
            ConfigService configService,
            Messages messages,
            UpdateChecker updateChecker,
            TaskScheduler scheduler) {
        this.playerManager = playerManager;
        this.settingsManager = settingsManager;
        this.navigatorService = navigatorService;
        this.spawnService = spawnService;
        this.worldStorage = worldStorage;
        this.playerLookupService = playerLookupService;
        this.noClipService = noClipService;
        this.configService = configService;
        this.messages = messages;
        this.updateChecker = updateChecker;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerJoinMessage(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String message = configService.current().settings().joinQuitMessages()
                ? messages.getString("player_join", player, Map.entry("%player%", player.getName()))
                : null;
        event.setJoinMessage(message);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerLookupService.cacheUser(player.getUniqueId(), player.getName());

        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(playerManager.getPlayerStorage().createBuildPlayer(player));
        manageHidePlayer(player, buildPlayer);
        manageSettings(player, buildPlayer.getSettings());
        teleportToCorrectLocation(player, buildPlayer);
        navigatorService.giveNavigator(player);

        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld != null) {
            WorldData worldData = buildWorld.getData();
            if (!worldData.get(WorldDataKey.PHYSICS) && player.hasPermission("buildsystem.physics.message")) {
                messages.sendMessage(player, "physics_deactivated_in_world", Map.entry("%world%", worldName));
            }

            if (configService.current().settings().archive().vanish()
                    && !worldData.get(WorldDataKey.STATUS).isBuildingAllowed()) {
                player.addPotionEffect(
                        new PotionEffect(XPotion.INVISIBILITY.get(), PotionEffect.INFINITE_DURATION, 0, false, false),
                        false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        }

        if (player.hasPermission("buildsystem.updates")) {
            performUpdateCheck(player);
        }
    }

    /**
     * Teleports the player to the correct location.
     *
     * <ul>
     *   <li>If the spawn exists and {@link SettingsImpl#isSpawnTeleport()} is enabled for the player, the player will
     *       be teleported to the spawn
     *   <li>If the player has a {@link LogoutLocation}, teleport to that location
     *   <li>Otherwise, do nothing
     * </ul>
     *
     * @param player The player to teleport
     * @param buildPlayer The build-player for the given player
     */
    private void teleportToCorrectLocation(Player player, BuildPlayerImpl buildPlayer) {
        if (buildPlayer.getSettings().isSpawnTeleport() && spawnService.spawnExists()) {
            spawnService.teleport(player);
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
        scheduler.runLater(
                () -> {
                    Location location = logoutLocation.location();
                    if (location != null) {
                        PaperLib.teleportAsync(player, location);
                    }
                },
                delay);
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
     * @param player The player to activate the features for
     * @param settings The player's settings
     */
    private void manageSettings(Player player, Settings settings) {
        if (settings.isNoClip()) {
            noClipService.startNoClip(player);
        }

        if (settings.isScoreboard()) {
            settingsManager.displayScoreboard(player);
            settingsManager.forceUpdateSidebar(player);
        }

        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }
    }

    private void performUpdateCheck(Player player) {
        if (!configService.current().settings().updateChecker()) {
            return;
        }

        updateChecker.requestUpdateCheck().whenComplete((result, e) -> {
            if (result.requiresUpdate()) {
                StringBuilder stringBuilder = new StringBuilder();
                messages.getStringList("update_available", player)
                        .forEach(line -> stringBuilder
                                .append(line.replace("%new_version%", result.getNewestVersion())
                                        .replace("%current_version%", updateChecker.getCurrentVersion()))
                                .append("\n"));
                player.sendMessage(stringBuilder.toString());
            }
        });
    }
}
