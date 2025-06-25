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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import org.bukkit.entity.Player;

public class EditSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    public EditSubCommand(BuildSystemPlugin plugin, String worldName) {
        this.plugin = plugin;
        this.buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(worldName);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, getArgument().getPermission())) {
            Messages.sendPermissionError(player);
            return;
        }

        if (args.length > 2) {
            Messages.sendMessage(player, "worlds_edit_usage");
            return;
        }

        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_edit_unknown_world");
            return;
        }

        if (buildWorld.isLoaded()) {
            plugin.getEditInventory().openInventory(player, buildWorld);
        } else {
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            player.sendTitle(" ", Messages.getString("world_not_loaded", player), 5, 70, 20);
        }
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.EDIT;
    }
}