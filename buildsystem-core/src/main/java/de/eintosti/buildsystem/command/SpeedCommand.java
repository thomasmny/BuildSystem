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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.player.settings.SpeedInventory;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SpeedCommand implements CommandExecutor {

    private static final float INVALID_SPEED = -1.0f;

    private final BuildSystemPlugin plugin;

    public SpeedCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("speed").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        if (!player.hasPermission("buildsystem.speed")) {
            Messages.sendPermissionError(player);
            return true;
        }

        switch (args.length) {
            case 0:
                new SpeedInventory(plugin).openInventory(player);
                break;
            case 1:
                String speedString = args[0];
                float speed = switch (speedString) {
                    case "1" -> 0.2f;
                    case "2" -> 0.4f;
                    case "3" -> 0.6f;
                    case "4" -> 0.8f;
                    case "5" -> 1.0f;
                    default -> INVALID_SPEED;
                };

                if (speed == INVALID_SPEED) {
                    Messages.sendMessage(player, "speed_usage");
                    return true;
                }

                setSpeed(player, speed, speedString);
                break;
            default:
                Messages.sendMessage(player, "speed_usage");
                break;
        }

        return true;
    }

    private void setSpeed(Player player, float speed, String speedString) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            Messages.sendMessage(player, "speed_set_flying", Map.entry("%speed%", speedString));
        } else {
            player.setWalkSpeed(speed);
            Messages.sendMessage(player, "speed_set_walking", Map.entry("%speed%", speedString));
        }
    }
}