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

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

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
     * Resolves a player name as {@link PlayerLookupService#resolve} does. When the name is unknown, {@code notFoundKey}
     * is sent and the player's inventory closed instead.
     */
    protected void resolvePlayer(
            PlayerLookupService lookup, Player player, String name, String notFoundKey, Consumer<Builder> onFound) {
        lookup.resolve(name, onFound, () -> {
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
        Completions.addWorldNames(
                args[1],
                worldService.getWorldStorage(),
                world -> {
                    String worldPermission = world.getData().get(WorldDataKey.PERMISSION);
                    return (worldPermission.equals("-") || player.hasPermission(worldPermission))
                            && world.getPermissions()
                                    .canPerformCommand(player, getArgument().getPermission());
                },
                result);
        return result;
    }
}
