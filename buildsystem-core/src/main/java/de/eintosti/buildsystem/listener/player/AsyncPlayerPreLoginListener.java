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

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AsyncPlayerPreLoginListener implements Listener {

    private final PlayerStorage playerStorage;
    private final SpawnService spawnService;
    private final WorldStorage worldStorage;
    private final TaskScheduler scheduler;

    public AsyncPlayerPreLoginListener(
            PlayerStorage playerStorage,
            SpawnService spawnService,
            WorldStorage worldStorage,
            TaskScheduler scheduler) {
        this.playerStorage = playerStorage;
        this.spawnService = spawnService;
        this.worldStorage = worldStorage;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        BuildPlayerImpl buildPlayer = BuildPlayerImpl.of(playerStorage.getBuildPlayer(uuid));
        if (buildPlayer == null) {
            return;
        }

        Settings settings = buildPlayer.getSettings();
        if (settings.isSpawnTeleport() && spawnService.spawnExists()) {
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
            scheduler.run(() -> buildWorld.getLoader().load());
        }
    }
}
