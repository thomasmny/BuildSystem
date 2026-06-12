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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.player.menu.SpeedMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SpeedCommand extends CommandBase {

    private static final float INVALID_SPEED = -1.0f;

    public SpeedCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, "buildsystem.speed")) {
            return;
        }

        switch (args.length) {
            case 0:
                new SpeedMenu(plugin, player).open(player);
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
                    messages.sendMessage(player, "speed_usage");
                    return;
                }

                setSpeed(player, speed, speedString);
                break;
            default:
                messages.sendMessage(player, "speed_usage");
                break;
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            list.add(String.valueOf(i));
        }
        return list;
    }

    private void setSpeed(Player player, float speed, String speedString) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            messages.sendMessage(player, "speed_set_flying", Map.entry("%speed%", speedString));
        } else {
            player.setWalkSpeed(speed);
            messages.sendMessage(player, "speed_set_walking", Map.entry("%speed%", speedString));
        }
    }
}
