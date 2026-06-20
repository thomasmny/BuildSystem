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
import java.util.*;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generic dispatcher for slash-command subcommands. The command name is matched case-insensitively; worldName is
 * resolved as {@code args[1]} when present, otherwise the player's current world name.
 */
@NullMarked
public final class SubCommandDispatcher {

    private final Map<String, SubCommand> byName;
    private final DynamicSubCommands dynamic;
    private final Messages messages;

    public SubCommandDispatcher(Messages messages, List<SubCommand> subCommands) {
        this(messages, subCommands, DynamicSubCommands.NONE);
    }

    /**
     * @param dynamic Runtime-derived subcommands (e.g. per-category shortcuts) consulted only after the static ones, so
     *     a dynamic name never shadows a real subcommand
     */
    public SubCommandDispatcher(Messages messages, List<SubCommand> subCommands, DynamicSubCommands dynamic) {
        this.messages = messages;
        this.dynamic = dynamic;
        this.byName = new LinkedHashMap<>();
        for (SubCommand cmd : subCommands) {
            byName.put(cmd.getArgument().getName().toLowerCase(Locale.ROOT), cmd);
        }
    }

    /**
     * Dispatches to the matched subcommand and returns {@code true} if a subcommand was found (even if it rejected the
     * invocation internally).
     */
    public boolean dispatch(Player player, String[] args) {
        SubCommand subCommand = resolve(args[0]);
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
     * Resolves a typed name to its subcommand. Static subcommands are checked first, so a dynamic shortcut (e.g. a
     * category named {@code delete}) never overrides a real subcommand of the same name.
     */
    private @Nullable SubCommand resolve(String name) {
        SubCommand subCommand = byName.get(name.toLowerCase(Locale.ROOT));
        return subCommand != null ? subCommand : dynamic.resolve(name).orElse(null);
    }

    /**
     * Returns tab-completion candidates.
     *
     * <ul>
     *   <li>args.length == 1: static subcommand names the player has permission for, plus the dynamic shortcut names the
     *       player can access (minus any whose name a static subcommand already owns)
     *   <li>args.length >= 2: delegates to the matched subcommand's {@code complete()}
     * </ul>
     */
    public List<String> complete(Player player, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            Set<String> staticNames = new HashSet<>();

            for (SubCommand cmd : byName.values()) {
                String name = cmd.getArgument().getName();
                staticNames.add(name.toLowerCase(Locale.ROOT));
                String permission = cmd.getArgument().getPermission();
                if ((permission == null || player.hasPermission(permission)) && matchesPrefix(name, prefix)) {
                    result.add(name);
                }
            }

            for (SubCommand cmd : dynamic.available(player)) {
                String name = cmd.getArgument().getName();
                // A static subcommand owns this name; its shortcut is shadowed and must not appear.
                if (staticNames.contains(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (matchesPrefix(name, prefix)) {
                    result.add(name);
                }
            }
            return result;
        }

        SubCommand subCommand = resolve(args[0]);
        return subCommand != null ? subCommand.complete(player, args) : List.of();
    }

    private static boolean matchesPrefix(String name, String prefix) {
        return prefix.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(prefix);
    }
}
