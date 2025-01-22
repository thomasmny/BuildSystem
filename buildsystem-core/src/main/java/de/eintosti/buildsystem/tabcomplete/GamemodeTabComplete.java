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
package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GamemodeTabComplete extends ArgumentSorter implements TabCompleter {

    public GamemodeTabComplete(BuildSystem plugin) {
        plugin.getCommand("gamemode").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (!(sender instanceof Player)) {
            return arrayList;
        }

        Player player = (Player) sender;
        if (args.length == 1) {
            Arrays.stream(GameMode.values())
                    .map(gameMode -> gameMode.name().toLowerCase(Locale.ROOT))
                    .filter(gameModeName -> player.hasPermission(String.format("buildsystem.gamemode.%s", gameModeName)))
                    .forEach(gameModeName -> addArgument(args[0], gameModeName, arrayList));
        } else if (args.length == 2) {
            String gameModeName;
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "survival":
                case "s":
                case "0":
                    gameModeName = GameMode.SURVIVAL.name().toLowerCase(Locale.ROOT);
                    break;
                case "creative":
                case "c":
                case "1":
                    gameModeName = GameMode.CREATIVE.name().toLowerCase(Locale.ROOT);
                    break;
                case "adventure":
                case "a":
                case "2":
                    gameModeName = GameMode.ADVENTURE.name().toLowerCase(Locale.ROOT);
                    break;
                case "spectator":
                case "sp":
                case "3":
                    gameModeName = GameMode.SPECTATOR.name().toLowerCase(Locale.ROOT);
                    break;
                default:
                    return arrayList;
            }

            if (player.hasPermission(String.format("buildsystem.gamemode.%s.other", gameModeName))) {
                Bukkit.getOnlinePlayers().forEach(pl -> addArgument(args[1], pl.getName(), arrayList));
            }
        }

        return arrayList;
    }
}