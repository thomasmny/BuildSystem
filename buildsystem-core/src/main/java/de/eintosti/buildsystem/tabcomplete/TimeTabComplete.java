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
import de.eintosti.buildsystem.api.storage.WorldStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimeTabComplete extends ArgumentSorter implements TabCompleter {

    private final WorldStorage worldStorage;

    public TimeTabComplete(BuildSystemPlugin plugin) {
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getCommand("day").setTabCompleter(this);
        plugin.getCommand("night").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (!(sender instanceof Player)) {
            return arrayList;
        }

        Player player = (Player) sender;
        String labelLowerCase = label.toLowerCase(Locale.ROOT);
        switch (labelLowerCase) {
            case "day":
            case "night":
                worldStorage.getBuildWorlds().stream()
                        .filter(world -> world.getPermissions().canPerformCommand(player, "buildsystem." + labelLowerCase))
                        .forEach(world -> addArgument(args[0], world.getName(), arrayList));
                break;
        }

        return arrayList;
    }
}