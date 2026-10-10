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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsSubCommand extends WorldSubCommand {

    private final BackupServiceImpl backupService;
    private final Menus menus;

    public BackupsSubCommand(
            Messages messages, WorldServiceImpl worldService, BackupServiceImpl backupService, Menus menus) {
        super(messages, worldService, WorldTarget.current("worlds_backup_world_not_imported"));
        this.backupService = backupService;
        this.menus = menus;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        switch (args.length) {
            case 1 -> {
                player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
                menus.openBackups(buildWorld, player);
            }
            case 2 -> {
                if (args[1].equalsIgnoreCase("create")) {
                    if (!player.hasPermission(Permissions.BACKUP_CREATE)) {
                        messages.sendPermissionError(player);
                        return;
                    }

                    backupService.backup(player, buildWorld);
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

        if (player.hasPermission(Permissions.BACKUP_CREATE)) {
            List<String> result = new ArrayList<>();
            Completions.addMatching(args[1], "create", result);
            return result;
        }

        return List.of();
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.BACKUP;
    }
}
