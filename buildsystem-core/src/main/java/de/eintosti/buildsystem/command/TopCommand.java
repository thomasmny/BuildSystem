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
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.lifecycle.WorldTeleporterImpl;
import io.papermc.lib.PaperLib;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TopCommand extends CommandBase {

    public TopCommand(Messages messages, Logger logger) {
        super(messages, logger, true);
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, "buildsystem.top")) {
            return;
        }

        if (args.length != 0) {
            messages.sendMessage(player, "top_usage");
            return;
        }

        sendToTop(player);
    }

    private void sendToTop(Player player) {
        Location playerLocation = player.getLocation();
        Location blockLocation = player.getWorld()
                .getHighestBlockAt(playerLocation.getBlockX(), playerLocation.getBlockZ())
                .getLocation();

        boolean failed = !WorldTeleporterImpl.isSafeLocation(blockLocation)
                || blockLocation.getBlock().getY() < playerLocation.getBlock().getY();
        if (failed) {
            messages.sendMessage(player, "top_failed");
            return;
        }

        PaperLib.teleportAsync(player, blockLocation.add(0.5, 1, 0.5)).whenComplete((completed, throwable) -> {
            if (!completed) {
                return;
            }
            XSound.ENTITY_ZOMBIE_INFECT.play(player);
            messages.sendMessage(player, "top_teleported");
        });
    }
}
