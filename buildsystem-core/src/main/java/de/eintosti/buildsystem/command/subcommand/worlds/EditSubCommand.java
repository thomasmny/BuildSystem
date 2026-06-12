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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.world.menu.EditMenu;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class EditSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;


    public EditSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = GuardedWorldCommand.requireWorld(plugin, player, worldName, args, 2, getArgument(), "worlds_edit");
        if (buildWorld == null) {
            return;
        }

        if (buildWorld.isLoaded()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            new EditMenu(plugin, buildWorld, player).open(player);
        } else {
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            player.sendTitle(" ", plugin.getMessages().getString("world_not_loaded", player), 5, 70, 20);
        }
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        WorldStorage ws = plugin.getWorldService().getWorldStorage();
        return WorldsCompletions.permittedWorldNames(player, ws, getArgument().getPermission(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.EDIT;
    }
}