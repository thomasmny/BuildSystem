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
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ItemSubCommand extends AbstractSubCommand {

    private final NavigatorItems navigatorItems;

    public ItemSubCommand(Messages messages, WorldServiceImpl worldService, NavigatorItems navigatorItems) {
        super(messages, worldService);
        this.navigatorItems = navigatorItems;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        player.getInventory().addItem(navigatorItems.create(player));
        messages.sendMessage(player, "worlds_item_receive");
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.ITEM;
    }
}
