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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
public class GamemodeCommand extends CommandBase {

    public GamemodeCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (args.length == 0) {
            messages.sendMessage(player, "gamemode_usage");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "survival":
            case "s":
            case "0":
                setGamemode(player, args, GameMode.SURVIVAL, messages.getString("gamemode_survival", player));
                break;
            case "creative":
            case "c":
            case "1":
                setGamemode(player, args, GameMode.CREATIVE, messages.getString("gamemode_creative", player));
                break;
            case "adventure":
            case "a":
            case "2":
                setGamemode(player, args, GameMode.ADVENTURE, messages.getString("gamemode_adventure", player));
                break;
            case "spectator":
            case "sp":
            case "3":
                setGamemode(player, args, GameMode.SPECTATOR, messages.getString("gamemode_spectator", player));
                break;
            default:
                messages.sendMessage(player, "gamemode_usage");
                break;
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            Arrays.stream(GameMode.values())
                    .map(gameMode -> gameMode.name().toLowerCase(Locale.ROOT))
                    .filter(gameModeName -> player.hasPermission("buildsystem.gamemode.%s".formatted(gameModeName)))
                    .forEach(gameModeName -> addArgument(args[0], gameModeName, list));
        } else if (args.length == 2) {
            String gameModeName =
                    switch (args[0].toLowerCase(Locale.ROOT)) {
                        case "survival", "s", "0" -> GameMode.SURVIVAL.name().toLowerCase(Locale.ROOT);
                        case "creative", "c", "1" -> GameMode.CREATIVE.name().toLowerCase(Locale.ROOT);
                        case "adventure", "a", "2" -> GameMode.ADVENTURE.name().toLowerCase(Locale.ROOT);
                        case "spectator", "sp", "3" -> GameMode.SPECTATOR.name().toLowerCase(Locale.ROOT);
                        default -> null;
                    };

            if (gameModeName != null && player.hasPermission("buildsystem.gamemode.%s.other".formatted(gameModeName))) {
                Bukkit.getOnlinePlayers().forEach(pl -> addArgument(args[1], pl.getName(), list));
            }
        }

        return list;
    }

    private void setGamemode(Player player, String[] args, GameMode gameMode, String gameModeName) {
        switch (args.length) {
            case 1 -> setPlayerGamemode(player, gameMode, gameModeName);
            case 2 -> setTargetGamemode(player, args, gameMode, gameModeName);
            default -> messages.sendMessage(player, "gamemode_usage");
        }
    }

    private void setPlayerGamemode(Player player, GameMode gameMode, String gameModeName) {
        if (!requirePermission(
                player, "buildsystem.gamemode.%s".formatted(gameMode.name().toLowerCase(Locale.ROOT)))) {
            return;
        }

        player.setGameMode(gameMode);
        messages.sendMessage(player, "gamemode_set_self", Map.entry("%gamemode%", gameModeName));
    }

    private void setTargetGamemode(Player player, String[] args, GameMode gameMode, String gameModeName) {
        if (!requirePermission(
                player,
                "buildsystem.gamemode.%s.other".formatted(gameMode.name().toLowerCase(Locale.ROOT)))) {
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.sendMessage(player, "gamemode_player_not_found");
            return;
        }

        target.setGameMode(gameMode);
        messages.sendMessage(target, "gamemode_set_self", Map.entry("%gamemode%", gameModeName));
        messages.sendMessage(
                player,
                "gamemode_set_other",
                Map.entry("%target%", target.getName()),
                Map.entry("%gamemode%", gameModeName));
    }
}
