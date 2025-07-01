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

import de.eintosti.buildsystem.BuildSystemPlugin;
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
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GamemodeTabComplete extends ArgumentSorter implements TabCompleter {

    public GamemodeTabComplete(BuildSystemPlugin plugin) {
        plugin.getCommand("gamemode").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (!(sender instanceof Player player)) {
            return arrayList;
        }

        if (args.length == 1) {
            Arrays.stream(GameMode.values())
                    .map(gameMode -> gameMode.name().toLowerCase(Locale.ROOT))
                    .filter(gameModeName -> player.hasPermission("buildsystem.gamemode.%s".formatted(gameModeName)))
                    .forEach(gameModeName -> addArgument(args[0], gameModeName, arrayList));
        } else if (args.length == 2) {
            String gameModeName = switch (args[0].toLowerCase(Locale.ROOT)) {
                case "survival", "s", "0" -> GameMode.SURVIVAL.name().toLowerCase(Locale.ROOT);
                case "creative", "c", "1" -> GameMode.CREATIVE.name().toLowerCase(Locale.ROOT);
                case "adventure", "a", "2" -> GameMode.ADVENTURE.name().toLowerCase(Locale.ROOT);
                case "spectator", "sp", "3" -> GameMode.SPECTATOR.name().toLowerCase(Locale.ROOT);
                default -> null;
            };

            if (gameModeName != null && player.hasPermission("buildsystem.gamemode.%s.other".formatted(gameModeName))) {
                Bukkit.getOnlinePlayers().forEach(pl -> addArgument(args[1], pl.getName(), arrayList));
            }
        }

        return arrayList;
    }
}