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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.command.subcommand.SubCommand;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * A source of subcommands that are not known up front but derived from runtime state — the {@code /worlds <category>}
 * shortcuts generated from the navigator categories. The {@link SubCommandDispatcher} consults its static subcommands
 * first, so a dynamic shortcut never shadows a real subcommand of the same name.
 */
@NullMarked
public interface DynamicSubCommands {

    /**
     * An empty source, used by dispatchers that have no dynamic subcommands.
     */
    DynamicSubCommands NONE = new DynamicSubCommands() {
        @Override
        public Optional<SubCommand> resolve(String name) {
            return Optional.empty();
        }

        @Override
        public List<SubCommand> available(Player player) {
            return List.of();
        }
    };

    /**
     * Resolves the dynamic subcommand a typed name maps to, if any. Called only after the static subcommands have been
     * checked, so the returned command never overrides a real subcommand.
     *
     * @param name The typed (raw-case) name
     * @return The matching subcommand, or {@link Optional#empty()} if none
     */
    Optional<SubCommand> resolve(String name);

    /**
     * The dynamic subcommands a player may tab-complete — already filtered to the ones the player has access to. The
     * dispatcher additionally drops any whose name collides with a static subcommand.
     *
     * @param player The player completing the command
     * @return The available dynamic subcommands
     */
    List<SubCommand> available(Player player);
}
