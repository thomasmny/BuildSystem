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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsSubCommand extends AbstractSubCommand {

    private final BackupServiceImpl backupService;
    private final Menus menus;

    public BackupsSubCommand(
            Messages messages, WorldServiceImpl worldService, BackupServiceImpl backupService, Menus menus) {
        super(messages, worldService);
        this.backupService = backupService;
        this.menus = menus;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld =
                worldService.getWorldStorage().getBuildWorld(player.getWorld().getName());
        if (buildWorld != null
                && !buildWorld
                        .getPermissions()
                        .canPerformCommand(player, getArgument().getPermission())) {
            messages.sendPermissionError(player);
            return;
        }

        if (buildWorld == null) {
            messages.sendMessage(player, "worlds_backup_world_not_imported");
            return;
        }

        switch (args.length) {
            case 1 -> {
                XSound.BLOCK_CHEST_OPEN.play(player);
                menus.openBackups(buildWorld, player);
            }
            case 2 -> {
                if (args[1].equalsIgnoreCase("create")) {
                    if (!player.hasPermission(getArgument().getPermission() + ".create")) {
                        messages.sendPermissionError(player);
                        return;
                    }

                    Entry<String, Object> worldNamePlaceholder = Map.entry("%world%", buildWorld.getName());
                    backupService.backup(
                            buildWorld,
                            () -> messages.sendMessage(player, "worlds_backup_created", worldNamePlaceholder),
                            () -> messages.sendMessage(player, "worlds_backup_failed", worldNamePlaceholder));
                } else {
                    messages.sendMessage(player, "worlds_backup_usage");
                }
            }
            default -> {
                messages.sendMessage(player, "worlds_backup_usage");
            }
        }
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        if (player.hasPermission(getArgument().getPermission() + ".create")) {
            List<String> result = new ArrayList<>();
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
