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
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class WorldsCompletions {

    private WorldsCompletions() {}

    /**
     * Returns world names the player may act on, filtered by world-level permission and a command-specific permission (e.g. "buildsystem.edit").
     */
    static List<String> permittedWorldNames(
            Player player, WorldStorage worldStorage, String commandPermission, String input) {
        List<String> result = new ArrayList<>();
        for (BuildWorld world : worldStorage.getBuildWorlds()) {
            String worldPerm = world.getData().permission().get();
            if ((player.hasPermission(worldPerm) || worldPerm.equalsIgnoreCase("-"))
                    && world.getPermissions().canPerformCommand(player, commandPermission)) {
                addIfStartsWith(input, world.getName(), result);
            }
        }
        return result;
    }

    /**
     * Returns world names the player can delete (no world-level access check, only command permission), matching the original delete completion branch.
     */
    static List<String> deletableWorldNames(Player player, WorldStorage worldStorage, String input) {
        List<String> result = new ArrayList<>();
        for (BuildWorld world : worldStorage.getBuildWorlds()) {
            if (world.getPermissions().canPerformCommand(player, "buildsystem.delete")) {
                addIfStartsWith(input, world.getName(), result);
            }
        }
        return result;
    }

    static void addIfStartsWith(String input, String candidate, List<String> result) {
        if (input.isEmpty() || candidate.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
            result.add(candidate);
        }
    }
}
