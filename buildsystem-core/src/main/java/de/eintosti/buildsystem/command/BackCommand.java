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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackCommand extends CommandBase {

    private final PlayerStorage playerStorage;

    public BackCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
        this.playerStorage = plugin.getPlayerService().getPlayerStorage();
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, "buildsystem.back")) {
            return;
        }

        if (args.length == 0) {
            teleportBack(player);
        } else {
            messages.sendMessage(player, "back_usage");
        }
    }

    private void teleportBack(Player player) {
        BuildPlayer buildPlayer = playerStorage.getBuildPlayer(player);
        Location previousLocation = buildPlayer.getPreviousLocation();
        if (previousLocation == null) {
            messages.sendMessage(player, "back_failed");
            return;
        }

        PaperLib.teleportAsync(player, previousLocation)
                .whenComplete((completed, throwable) -> {
                    if (!completed) {
                        return;
                    }
                    XSound.ENTITY_ZOMBIE_INFECT.play(player);
                    messages.sendMessage(player, "back_teleported");
                    buildPlayer.setPreviousLocation(null);
                });
    }
}
