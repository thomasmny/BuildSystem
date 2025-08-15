/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.InventoryUtils;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkullCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;

    public SkullCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("skull").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        if (!player.hasPermission("buildsystem.skull")) {
            Messages.sendPermissionError(player);
            return true;
        }

        switch (args.length) {
            case 0 -> {
                addSkull(player, "§b" + player.getName(), Profileable.of(player));
                Messages.sendMessage(player, "skull_player_received", Map.entry("%player%", player.getName()));
            }
            case 1 -> {
                String identifier = args[0];
                if (identifier.length() > 16) {
                    addSkull(player, Messages.getString("custom_skull_item", player), Profileable.detect(identifier));
                    Messages.sendMessage(player, "skull_custom_received");
                } else {
                    addSkull(player, "§b" + identifier, Profileable.detect(identifier));
                    Messages.sendMessage(player, "skull_player_received", Map.entry("%player%", identifier));
                }
            }
            default -> {
                Messages.sendMessage(player, "skull_usage");
            }
        }
        return true;
    }

    private void addSkull(Player player, String displayName, Profileable profileable) {
        player.getInventory().addItem(InventoryUtils.createSkull(displayName, profileable));
    }
}