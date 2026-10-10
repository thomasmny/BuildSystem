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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Which world a {@link WorldSubCommand} acts on, and the checks that run before it does.
 */
@NullMarked
public sealed interface WorldTarget {

    /**
     * The world typed after the subcommand, or the player's current world when none is typed. The checks run in this
     * order, each answering the player when it fails:
     *
     * <ol>
     *   <li>the name is resolved as {@link WorldServiceImpl#resolveWorldName} describes, which answers an ambiguous
     *       name itself;
     *   <li>the permission, when the world exists;
     *   <li>the argument count, against {@code <prefix>_usage};
     *   <li>that the world exists, against {@code <prefix>_unknown_world}.
     * </ol>
     *
     * @param maxArgs The most arguments the subcommand accepts, the subcommand's own name included
     * @param messageKeyPrefix The message key prefix, e.g. {@code "worlds_edit"}
     */
    static WorldTarget argument(int maxArgs, String messageKeyPrefix) {
        return new Typed(maxArgs, messageKeyPrefix);
    }

    /**
     * The world the player stands in. It must be a build world, answered with {@code missingKey}, and then the player
     * needs the permission.
     */
    static WorldTarget current(String missingKey) {
        return new Current(missingKey);
    }

    /**
     * {@return the world to act on, or {@code null} if a check failed and the player was told why}
     *
     * @param worldName The typed world name, or the current world's when none was typed
     * @param permission The subcommand's permission
     */
    @Nullable BuildWorld resolve(
            Player player,
            String worldName,
            String[] args,
            Messages messages,
            WorldServiceImpl worldService,
            @Nullable String permission);

    record Typed(int maxArgs, String messageKeyPrefix) implements WorldTarget {

        @Override
        public @Nullable BuildWorld resolve(
                Player player,
                String worldName,
                String[] args,
                Messages messages,
                WorldServiceImpl worldService,
                @Nullable String permission) {
            String resolvedName = worldService.resolveWorldName(player, worldName, permission);
            if (resolvedName == null) {
                return null;
            }

            BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(resolvedName);
            if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, permission)) {
                messages.sendPermissionError(player);
                return null;
            }
            if (args.length > maxArgs) {
                messages.sendMessage(player, messageKeyPrefix + "_usage");
                return null;
            }
            if (buildWorld == null) {
                messages.sendMessage(player, messageKeyPrefix + "_unknown_world");
            }
            return buildWorld;
        }
    }

    record Current(String missingKey) implements WorldTarget {

        @Override
        public @Nullable BuildWorld resolve(
                Player player,
                String worldName,
                String[] args,
                Messages messages,
                WorldServiceImpl worldService,
                @Nullable String permission) {
            BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(player.getWorld());
            if (buildWorld == null) {
                messages.sendMessage(player, missingKey);
                return null;
            }
            if (!buildWorld.getPermissions().canPerformCommand(player, permission)) {
                messages.sendPermissionError(player);
                return null;
            }
            return buildWorld;
        }
    }
}
