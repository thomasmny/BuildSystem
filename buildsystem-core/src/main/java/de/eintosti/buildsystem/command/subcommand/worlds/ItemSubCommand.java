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
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.config.Config.Settings.Navigator;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ItemSubCommand implements SubCommand {

    public ItemSubCommand() {
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            Messages.sendPermissionError(player);
            return;
        }

        ItemStack navigator = InventoryUtils.createItem(
                Navigator.item,
                Messages.getString("navigator_item", player)
        );
        player.getInventory().addItem(navigator);
        Messages.sendMessage(player, "worlds_item_receive");
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.ITEM;
    }
}