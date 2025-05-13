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
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    private final PlayerServiceImpl playerManager;
    private final WorldStorageImpl worldStorage;

    public PlayerTeleportListener(BuildSystemPlugin plugin) {
        this.playerManager = plugin.getPlayerService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            playerManager.getBuildPlayer(player).setPreviousLocation(event.getFrom());
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        World toWorld = to.getWorld();
        if (toWorld == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(toWorld);
        if (buildWorld == null) {
            return;
        }

        // Users can always teleport to the main server world
        if (Bukkit.getWorlds().get(0).equals(toWorld)) {
            return;
        }

        if (!WorldPermissionsImpl.of(buildWorld).canEnter(player)) {
            Messages.sendMessage(player, "worlds_tp_entry_forbidden");
            event.setCancelled(true);
        }
    }
}