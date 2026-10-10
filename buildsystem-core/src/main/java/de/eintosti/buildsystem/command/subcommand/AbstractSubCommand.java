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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base class for subcommands, holding the dependencies every implementation needs.
 */
@NullMarked
public abstract class AbstractSubCommand implements SubCommand {

    protected final Messages messages;
    protected final WorldServiceImpl worldService;

    protected AbstractSubCommand(Messages messages, WorldServiceImpl worldService) {
        this.messages = messages;
        this.worldService = worldService;
    }

    /**
     * Runs the shared preamble for subcommands that act on a named world: permission (checked before existence so
     * unpermitted players cannot probe which world names exist), argument count, then existence. A name typed without a
     * namespace is resolved as {@link WorldServiceImpl#resolveWorldName} describes.
     *
     * @param player The command sender
     * @param worldName The world name argument
     * @param args The raw command arguments
     * @param maxArgs The maximum permitted argument count (inclusive)
     * @param messageKeyPrefix The message key prefix, e.g. {@code "worlds_edit"}; {@code _usage}/{@code _unknown_world}
     *     are appended
     * @return The world if all checks pass, otherwise {@code null} (an error message has already been sent)
     */
    protected @Nullable BuildWorld requireWorld(
            Player player, String worldName, String[] args, int maxArgs, String messageKeyPrefix) {
        String resolvedName =
                worldService.resolveWorldName(player, worldName, getArgument().getPermission());
        if (resolvedName == null) {
            return null;
        }

        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(resolvedName);
        if (buildWorld != null
                && !buildWorld
                        .getPermissions()
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

    /**
     * The preamble for subcommands that act on the world the player stands in: permission first when it is a build
     * world, then existence.
     *
     * @param player The command sender
     * @param missingKey The message sent when the player is not in a build world
     * @return The world if both checks pass, otherwise {@code null} (an error message has already been sent)
     */
    protected @Nullable BuildWorld requireCurrentWorld(Player player, String missingKey) {
        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            messages.sendMessage(player, missingKey);
            return null;
        }

        if (!buildWorld.getPermissions().canPerformCommand(player, getArgument().getPermission())) {
            messages.sendPermissionError(player);
            return null;
        }
        return buildWorld;
    }

    /**
     * Resolves a player name as {@link PlayerLookupService#resolve} does, with {@code onFound} on the main thread. When
     * the name is unknown, {@code notFoundKey} is sent and the player's inventory closed instead.
     */
    protected void resolvePlayer(
            PlayerLookupService lookup,
            TaskScheduler scheduler,
            Player player,
            String name,
            String notFoundKey,
            Consumer<Builder> onFound) {
        lookup.resolve(name, scheduler.mainThread(), onFound, () -> {
            messages.sendMessage(player, notFoundKey);
            player.closeInventory();
        });
    }

    /**
     * Completes the world-name argument with the worlds the player may see and run this subcommand in.
     */
    protected List<String> completeWorldName(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        WorldStorageImpl worldStorage = worldService.getWorldStorage();
        for (BuildWorld world : worldStorage.getBuildWorlds()) {
            String worldPermission = world.getData().get(WorldDataKey.PERMISSION);
            String name = worldStorage.typedName(world.getName());
            if ((player.hasPermission(worldPermission) || worldPermission.equals("-"))
                    && world.getPermissions()
                            .canPerformCommand(player, getArgument().getPermission())
                    && StringUtil.startsWithIgnoreCase(name, args[1])) {
                result.add(name);
            }
        }
        return result;
    }
}
