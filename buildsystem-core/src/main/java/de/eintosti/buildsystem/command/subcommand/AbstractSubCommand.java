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
package de.eintosti.buildsystem.command.subcommand;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base class for subcommands, holding the dependencies every implementation needs.
 */
@NullMarked
public abstract class AbstractSubCommand implements SubCommand {

    protected final BuildSystemPlugin plugin;
    protected final Messages messages;

    protected AbstractSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    /**
     * Runs the shared preamble for subcommands that act on a named world: permission (checked before existence so
     * unpermitted players cannot probe which world names exist), argument count, then existence.
     *
     * @param player The command sender
     * @param worldName The world name argument
     * @param args The raw command arguments
     * @param maxArgs The maximum permitted argument count (inclusive)
     * @param messageKeyPrefix The message key prefix, e.g. {@code "worlds_edit"}; {@code _usage}/{@code _unknown_world}
     *     are appended
     * @return The world if all checks pass, otherwise {@code null} (an error message has already been sent)
     */
    @Nullable protected BuildWorld requireWorld(
            Player player, String worldName, String[] args, int maxArgs, String messageKeyPrefix) {
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(worldName);
        if (!WorldPermissionsImpl.of(plugin, buildWorld)
                .canPerformCommand(player, getArgument().getPermission())) {
            messages.sendPermissionError(player);
            return null;
        }

        if (args.length > maxArgs) {
            messages.sendMessage(player, messageKeyPrefix + "_usage");
            return null;
        }

        if (buildWorld == null) {
            messages.sendMessage(player, messageKeyPrefix + "_unknown_world");
            return null;
        }

        return buildWorld;
    }
}
