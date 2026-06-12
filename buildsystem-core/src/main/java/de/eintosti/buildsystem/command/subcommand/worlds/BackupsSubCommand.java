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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.world.backup.BackupsMenu;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;

    public BackupsSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(player.getWorld().getName());
        if (!WorldPermissionsImpl.of(plugin, buildWorld).canPerformCommand(player, getArgument().getPermission())) {
            plugin.getMessages().sendPermissionError(player);
            return;
        }

        if (buildWorld == null) {
            plugin.getMessages().sendMessage(player, "worlds_backup_world_not_imported");
            return;
        }

        switch (args.length) {
            case 1 -> {
                XSound.BLOCK_CHEST_OPEN.play(player);
                new BackupsMenu(plugin, buildWorld, player).open(player);
            }
            case 2 -> {
                if (args[1].equalsIgnoreCase("create")) {
                    if (!player.hasPermission(getArgument().getPermission() + ".create")) {
                        plugin.getMessages().sendPermissionError(player);
                        return;
                    }

                    Entry<String, Object> worldNamePlaceholder = Map.entry("%world%", buildWorld.getName());
                    plugin.getBackupService().backup(buildWorld,
                            () -> plugin.getMessages().sendMessage(player, "worlds_backup_created", worldNamePlaceholder),
                            () -> plugin.getMessages().sendMessage(player, "worlds_backup_failed", worldNamePlaceholder)
                    );
                } else {
                    plugin.getMessages().sendMessage(player, "worlds_backup_usage");
                }
            }
            default -> {
                plugin.getMessages().sendMessage(player, "worlds_backup_usage");
            }
        }
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        if (player.hasPermission(getArgument().getPermission() + ".create")) {
            List<String> result = new java.util.ArrayList<>();
            WorldsCompletions.addIfStartsWith(args[1], "create", result);
            return result;
        }
        return List.of();
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.BACKUP;
    }
}