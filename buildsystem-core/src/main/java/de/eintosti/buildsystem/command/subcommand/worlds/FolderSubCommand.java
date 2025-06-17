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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete.WorldsArgument;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map.Entry;
import org.bukkit.entity.Player;

public class FolderSubCommand implements SubCommand {

    private final WorldServiceImpl worldService;

    public FolderSubCommand(BuildSystemPlugin plugin) {
        this.worldService = plugin.getWorldService();
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length != 4) {
            Messages.sendMessage(player, "worlds_folder_usage");
            return;
        }

        String folderName = args[1];
        String operation = args[2].toLowerCase(Locale.ROOT);
        String worldName = args[3];

        FolderStorage folderStorage = worldService.getFolderStorage();
        Folder folder = folderStorage.getFolder(folderName);
        if (folder == null) {
            Messages.sendMessage(player, "worlds_folder_unknown_folder");
            return;
        }

        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_folder_unknown_world");
            return;
        }

        Entry<String, Object> folderPlaceholder = new AbstractMap.SimpleEntry<>("%folder%", folder.getName());
        Entry<String, Object> worldPlaceholder = new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName());

        switch (operation) {
            case "add": {
                if (folder.containsWorld(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_already_in_folder", folderPlaceholder, worldPlaceholder);
                    return;
                }

                if (folderStorage.isAssignedToAnyFolder(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_already_in_another_folder", worldPlaceholder);
                    return;
                }

                if (folder.getCategory() != NavigatorCategory.of(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_category_mismatch",
                            new AbstractMap.SimpleEntry<>("%folder_category%", folder.getCategory().name()),
                            new AbstractMap.SimpleEntry<>("%world_category%", NavigatorCategory.of(buildWorld).name())
                    );
                    return;
                }

                folder.addWorld(buildWorld);
                Messages.sendMessage(player, "worlds_folder_world_added_to_folder", folderPlaceholder, worldPlaceholder);
                break;
            }

            case "remove": {
                if (!folder.containsWorld(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_not_in_folder", folderPlaceholder, worldPlaceholder);
                    return;
                }

                folder.removeWorld(buildWorld);
                Messages.sendMessage(player, "worlds_folder_world_removed_from_folder", folderPlaceholder, worldPlaceholder);
                break;
            }

            default: {
                Messages.sendMessage(player, "worlds_folder_usage");
                break;
            }
        }
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.FOLDER;
    }
}