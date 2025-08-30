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
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.LogoutLocation;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.SpawnManager;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AsyncPlayerPreLoginListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final PlayerStorage playerStorage;
    private final SpawnManager spawnManager;
    private final WorldStorageImpl worldStorage;

    public AsyncPlayerPreLoginListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerStorage = plugin.getPlayerService().getPlayerStorage();
        this.spawnManager = plugin.getSpawnManager();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        BuildPlayer buildPlayer = playerStorage.getBuildPlayer(uuid);
        if (buildPlayer == null) {
            return;
        }

        Settings settings = buildPlayer.getSettings();
        if (settings.isSpawnTeleport() && spawnManager.spawnExists()) {
            return;
        }

        LogoutLocation logoutLocation = buildPlayer.getLogoutLocation();
        if (logoutLocation == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(logoutLocation.worldName());
        if (buildWorld == null) {
            buildPlayer.setLogoutLocation(null);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> buildWorld.getLoader().load());
        }
    }
}