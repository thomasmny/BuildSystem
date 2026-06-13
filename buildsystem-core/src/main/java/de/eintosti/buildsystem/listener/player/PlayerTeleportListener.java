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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerTeleportListener implements Listener {

    private final Messages messages;
    private final PlayerStorage playerStorage;
    private final WorldStorage worldStorage;

    public PlayerTeleportListener(BuildSystemPlugin plugin) {
        this.messages = plugin.getMessages();
        this.playerStorage = plugin.getPlayerService().getPlayerStorage();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            BuildPlayerImpl.of(playerStorage.getBuildPlayer(player)).setPreviousLocation(event.getFrom());
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
        if (Bukkit.getWorlds().getFirst().equals(toWorld)) {
            return;
        }

        if (!buildWorld.getPermissions().canEnter(player)) {
            messages.sendMessage(player, "worlds_tp_entry_forbidden");
            event.setCancelled(true);
        }
    }
}
