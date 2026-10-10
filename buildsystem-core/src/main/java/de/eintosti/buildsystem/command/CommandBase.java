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

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class CommandBase implements CommandExecutor, TabCompleter {

    protected final Messages messages;
    protected final Logger logger;
    private final boolean playerOnly;

    protected CommandBase(Messages messages, Logger logger, boolean playerOnly) {
        this.messages = messages;
        this.logger = logger;
        this.playerOnly = playerOnly;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (playerOnly) {
            if (!(sender instanceof Player player)) {
                messages.sendMessage(sender, "sender_not_player");
                return true;
            }
            run(player, label, args);
        } else {
            run(sender, label, args);
        }
        return true;
    }

    protected void run(Player player, String label, String[] args) {}

    protected void run(CommandSender sender, String label, String[] args) {}

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return sender instanceof Player player ? complete(player, label, args) : List.of();
    }

    protected List<String> complete(Player player, String label, String[] args) {
        return List.of();
    }

    protected boolean requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            messages.sendPermissionError(sender);
            return false;
        }
        return true;
    }

    /**
     * {@return the name of the world at {@code args[index]}, or of the player's current world when it is missing} A
     * typed name is resolved as {@link WorldServiceImpl#resolveWorldName} describes; {@code null} means it was
     * ambiguous and the player has been told.
     */
    protected @Nullable String worldNameFromArgs(
            Player player, String[] args, int index, WorldServiceImpl worldService, @Nullable String permission) {
        return args.length <= index
                ? WorldNames.of(player.getWorld())
                : worldService.resolveWorldName(player, args[index], permission);
    }
}
