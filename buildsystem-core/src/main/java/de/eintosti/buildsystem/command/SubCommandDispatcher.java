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
import de.eintosti.buildsystem.i18n.Messages;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Generic dispatcher for slash-command subcommands. The command name is matched
 * case-insensitively; worldName is resolved as {@code args[1]} when present,
 * otherwise the player's current world name.
 */
@NullMarked
public final class SubCommandDispatcher {

    private final Map<String, SubCommand> byName;
    private final Messages messages;

    public SubCommandDispatcher(Messages messages, List<SubCommand> subCommands) {
        this.messages = messages;
        this.byName = new LinkedHashMap<>();
        for (SubCommand cmd : subCommands) {
            byName.put(cmd.getArgument().getName().toLowerCase(Locale.ROOT), cmd);
        }
    }

    /**
     * Dispatches to the matched subcommand and returns {@code true} if a subcommand
     * was found (even if it rejected the invocation internally).
     */
    public boolean dispatch(Player player, String[] args) {
        SubCommand subCommand = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            messages.sendMessage(player, "worlds_unknown_command");
            return false;
        }
        // Convention: /worlds <sub> <world> — args[1] is the world name when present
        String worldName = args.length >= 2 ? args[1] : player.getWorld().getName();
        subCommand.execute(player, worldName, args);
        return true;
    }

    /**
     * Returns tab-completion candidates.
     * <ul>
     *   <li>args.length == 1: subcommand names the player has permission for</li>
     *   <li>args.length >= 2: delegates to the matched subcommand's {@code complete()}</li>
     * </ul>
     */
    public List<String> complete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (SubCommand cmd : byName.values()) {
                String permission = cmd.getArgument().getPermission();
                if (player.hasPermission(permission)) {
                    String name = cmd.getArgument().getName();
                    if (args[0].isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                        result.add(name);
                    }
                }
            }
            return result;
        }
        SubCommand subCommand = byName.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand != null) {
            return subCommand.complete(player, args);
        }
        return List.of();
    }
}
