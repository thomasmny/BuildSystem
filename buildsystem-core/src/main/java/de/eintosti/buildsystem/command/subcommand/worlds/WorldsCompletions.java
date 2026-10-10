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

import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class WorldsCompletions {

    private WorldsCompletions() {}

    /**
     * Returns world names the player can delete: those they hold {@code buildsystem.delete} for and that are not on the
     * deletion blacklist. Mirrors the checks {@code /worlds delete} enforces, so the completion never suggests a world
     * the command would refuse.
     */
    static List<String> deletableWorldNames(
            Player player, WorldServiceImpl worldService, Set<String> deletionBlacklist, String input) {
        List<String> result = new ArrayList<>();
        Completions.addWorldNames(
                input,
                worldService.getWorldStorage(),
                world -> !deletionBlacklist.contains(WorldNames.id(world.getName()))
                        && world.getPermissions().canPerformCommand(player, Permissions.DELETE),
                result);
        return result;
    }
}
