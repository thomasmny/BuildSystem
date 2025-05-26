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
package de.eintosti.buildsystem.command;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.storage.WorldStorage;
import de.eintosti.buildsystem.world.util.WorldTeleporter;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final WorldStorage worldStorage;

    public TopCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getCommand("top").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.top")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length != 0) {
            Messages.sendMessage(player, "top_usage");
            return true;
        }

        sendToTop(player);
        return true;
    }

    private void sendToTop(Player player) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());

        Location playerLocation = player.getLocation();
        Location blockLocation = player.getWorld()
                .getHighestBlockAt(playerLocation.getBlockX(), playerLocation.getBlockZ())
                .getLocation();

        boolean failed = !WorldTeleporter.of(buildWorld).isSafeLocation(blockLocation) || blockLocation.getBlock().getY() < playerLocation.getBlock().getY();
        if (failed) {
            Messages.sendMessage(player, "top_failed");
            return;
        }

        PaperLib.teleportAsync(player, blockLocation.add(0.5, 1, 0.5))
                .whenComplete((completed, throwable) -> {
                    if (!completed) {
                        return;
                    }
                    XSound.ENTITY_ZOMBIE_INFECT.play(player);
                    Messages.sendMessage(player, "top_teleported");
                });
    }
}