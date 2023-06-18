/*
 * Copyright (c) 2018-2023, Thomas Meaney
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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.world.BuildWorldManager;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TopCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;
    private final BuildWorldManager worldManager;

    public TopCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("top").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player"));
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
        World bukkitWorld = player.getWorld();
        Location playerLocation = player.getLocation();
        Location blockLocation = null;

        for (int y = bukkitWorld.getMaxHeight(); y > 0; y--) {
            Block block = bukkitWorld.getBlockAt(playerLocation.getBlockX(), y, playerLocation.getBlockZ());
            if (worldManager.isSafeLocation(block.getLocation()) && y > playerLocation.getY()) {
                blockLocation = block.getLocation();
                break;
            }
        }

        if (blockLocation != null && !Objects.equals(blockLocation.getBlock(), playerLocation.getBlock())) {
            PaperLib.teleportAsync(player, blockLocation.add(0.5, 0, 0.5));
            Messages.sendMessage(player, "top_teleported");
        } else {
            Messages.sendMessage(player, "top_failed");
        }
    }
}