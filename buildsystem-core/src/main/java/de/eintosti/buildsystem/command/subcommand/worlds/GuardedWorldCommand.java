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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Shared preamble for the {@code /worlds} subcommands that act on a named world.
 */
@NullMarked
final class GuardedWorldCommand {

    private GuardedWorldCommand() {}

    /**
     * Runs the shared preamble: permission (checked before existence so unpermitted players cannot probe which world names exist), argument count, then existence.
     *
     * @param plugin           The plugin
     * @param player           The command sender
     * @param worldName        The world name argument
     * @param args             The raw command arguments
     * @param maxArgs          The maximum permitted argument count (inclusive)
     * @param argument         The argument supplying the permission node
     * @param messageKeyPrefix The message key prefix, e.g. {@code "worlds_edit"}; {@code _usage}/{@code _unknown_world} are appended
     * @return The world if all checks pass, otherwise {@code null} (an error message has already been sent)
     */
    @Nullable
    static BuildWorld requireWorld(
            BuildSystemPlugin plugin,
            Player player,
            String worldName,
            String[] args,
            int maxArgs,
            Argument argument,
            String messageKeyPrefix) {
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(worldName);
        if (!WorldPermissionsImpl.of(plugin, buildWorld).canPerformCommand(player, argument.getPermission())) {
            plugin.getMessages().sendPermissionError(player);
            return null;
        }
        if (args.length > maxArgs) {
            plugin.getMessages().sendMessage(player, messageKeyPrefix + "_usage");
            return null;
        }
        if (buildWorld == null) {
            plugin.getMessages().sendMessage(player, messageKeyPrefix + "_unknown_world");
            return null;
        }
        return buildWorld;
    }
}
