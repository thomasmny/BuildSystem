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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GamemodeCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;

    public GamemodeCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("gamemode").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        if (args.length == 0) {
            sendUsageMessage(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "survival":
            case "s":
            case "0":
                setGamemode(player, args, GameMode.SURVIVAL, Messages.getString("gamemode_survival", player));
                break;
            case "creative":
            case "c":
            case "1":
                setGamemode(player, args, GameMode.CREATIVE, Messages.getString("gamemode_creative", player));
                break;
            case "adventure":
            case "a":
            case "2":
                setGamemode(player, args, GameMode.ADVENTURE, Messages.getString("gamemode_adventure", player));
                break;
            case "spectator":
            case "sp":
            case "3":
                setGamemode(player, args, GameMode.SPECTATOR, Messages.getString("gamemode_spectator", player));
                break;
            default:
                sendUsageMessage(player);
                break;
        }

        return true;
    }

    private void setGamemode(Player player, String[] args, GameMode gameMode, String gameModeName) {
        switch (args.length) {
            case 1 -> this.setPlayerGamemode(player, gameMode, gameModeName);
            case 2 -> this.setTargetGamemode(player, args, gameMode, gameModeName);
            default -> this.sendUsageMessage(player);
        }
    }

    private void sendUsageMessage(Player player) {
        Messages.sendMessage(player, "gamemode_usage");
    }

    private void setPlayerGamemode(Player player, GameMode gameMode, String gameModeName) {
        if (!player.hasPermission("buildsystem.gamemode.%s".formatted(gameMode.name().toLowerCase(Locale.ROOT)))) {
            Messages.sendPermissionError(player);
            return;
        }

        player.setGameMode(gameMode);
        Messages.sendMessage(player, "gamemode_set_self", Map.entry("%gamemode%", gameModeName));
    }

    private void setTargetGamemode(Player player, String[] args, GameMode gameMode, String gameModeName) {
        if (!player.hasPermission("buildsystem.gamemode.%s.other".formatted(gameMode.name().toLowerCase(Locale.ROOT)))) {
            Messages.sendPermissionError(player);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Messages.sendMessage(player, "gamemode_player_not_found");
            return;
        }

        target.setGameMode(gameMode);
        Messages.sendMessage(target, "gamemode_set_self", Map.entry("%gamemode%", gameModeName));
        Messages.sendMessage(player, "gamemode_set_other",
                Map.entry("%target%", target.getName()),
                Map.entry("%gamemode%", gameModeName)
        );
    }
}