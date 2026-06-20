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
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerRespawnListener implements Listener {

    private final SettingsService settingsManager;
    private final SpawnService spawnService;

    public PlayerRespawnListener(SettingsService settingsManager, SpawnService spawnService) {
        this.settingsManager = settingsManager;
        this.spawnService = spawnService;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isSpawnTeleport()) {
            return;
        }

        Location spawn = spawnService.getSpawn();
        if (spawn == null) {
            return;
        }

        event.setRespawnLocation(spawn);
    }
}
