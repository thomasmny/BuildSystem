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

/**
 * A subcommand that acts on one world. The {@link WorldTarget} picks the world and runs the checks, so
 * {@link #execute(Player, BuildWorld, String[])} only runs once they have passed.
 */
@NullMarked
public abstract class WorldSubCommand extends AbstractSubCommand {

    private final WorldTarget target;

    protected WorldSubCommand(Messages messages, WorldServiceImpl worldService, WorldTarget target) {
        super(messages, worldService);
        this.target = target;
    }

    @Override
    public final void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = target.resolve(
                player, worldName, args, messages, worldService, getArgument().getPermission());
        if (buildWorld != null) {
            execute(player, buildWorld, args);
        }
    }

    /**
     * Runs the subcommand on a world the player may use it on.
     *
     * @param player The command sender
     * @param buildWorld The world the {@link WorldTarget} resolved
     * @param args The raw command arguments, the subcommand's name first
     */
    protected abstract void execute(Player player, BuildWorld buildWorld, String[] args);
}
