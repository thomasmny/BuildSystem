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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FolderSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;
    private final FolderStorage folderStorage;

    public FolderSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldService = plugin.getWorldService();
        this.folderStorage = worldService.getFolderStorage();
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            Messages.sendMessage(player, "worlds_folder_usage");
            return;
        }

        String folderName = args[1];
        String operation = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "";

        Folder folder = this.folderStorage.getFolder(folderName);
        if (folder == null) {
            Messages.sendMessage(player, "worlds_folder_unknown_folder");
            return;
        }

        switch (operation) {
            case "add":
            case "remove": {
                if (args.length != 4) {
                    Messages.sendMessage(player, "worlds_folder_usage");
                    return;
                }
                handleWorldFolderOperation(player, folder, operation, args[3]);
                break;
            }

            case "setpermission": {
                handlePermissionInput(player, folder);
                break;
            }

            case "setproject": {
                handleProjectInput(player, folder);
                break;
            }

            case "setitem": {
                handleIconChange(player, folder);
                break;
            }

            case "delete": {
                handleDeletion(player, folder);
                break;
            }

            default: {
                Messages.sendMessage(player, "worlds_folder_usage");
                break;
            }
        }
    }

    private void handleWorldFolderOperation(Player player, Folder folder, String operation, String worldName) {
        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_folder_unknown_world");
            return;
        }

        Entry<String, Object> folderPlaceholder = Map.entry("%folder%", folder.getName());
        Entry<String, Object> worldPlaceholder = Map.entry("%world%", buildWorld.getName());

        switch (operation) {
            case "add":
                if (!player.hasPermission("buildsystem.folder.add")) {
                    Messages.sendPermissionError(player);
                    return;
                }

                if (folder.containsWorld(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_already_in_folder", folderPlaceholder, worldPlaceholder);
                    return;
                }

                if (buildWorld.isAssignedToFolder()) {
                    Messages.sendMessage(player, "worlds_folder_world_already_in_another_folder", worldPlaceholder);
                    return;
                }

                if (folder.getCategory() != NavigatorCategory.of(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_category_mismatch",
                            Map.entry("%folder_category%", folder.getCategory().name()),
                            Map.entry("%world_category%", NavigatorCategory.of(buildWorld).name())
                    );
                    return;
                }

                folder.addWorld(buildWorld);
                Messages.sendMessage(player, "worlds_folder_world_added_to_folder", folderPlaceholder, worldPlaceholder);
                break;

            case "remove":
                if (!player.hasPermission("buildsystem.folder.remove")) {
                    Messages.sendPermissionError(player);
                    return;
                }

                if (!folder.containsWorld(buildWorld)) {
                    Messages.sendMessage(player, "worlds_folder_world_not_in_folder", folderPlaceholder, worldPlaceholder);
                    return;
                }

                folder.removeWorld(buildWorld);
                Messages.sendMessage(player, "worlds_folder_world_removed_from_folder", folderPlaceholder, worldPlaceholder);
                break;
        }
    }

    private void handlePermissionInput(Player player, Folder folder) {
        if (!player.hasPermission("buildsystem.folder.setpermission")) {
            Messages.sendPermissionError(player);
            return;
        }

        new PlayerChatInput(this.plugin, player, "enter_world_permission", input -> {
            folder.setPermission(input.trim());

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            Messages.sendMessage(player, "worlds_folder_permission_set",
                    Map.entry("%folder%", folder.getName())
            );
        });
    }

    private void handleProjectInput(Player player, Folder folder) {
        if (!player.hasPermission("buildsystem.folder.setproject")) {
            Messages.sendPermissionError(player);
            return;
        }

        new PlayerChatInput(this.plugin, player, "enter_world_project", input -> {
            folder.setProject(input.trim());

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            Messages.sendMessage(player, "worlds_folder_project_set",
                    Map.entry("%folder%", folder.getName())
            );
        });
    }

    private void handleIconChange(Player player, Folder folder) {
        if (!player.hasPermission("buildsystem.folder.setitem")) {
            Messages.sendPermissionError(player);
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            Messages.sendMessage(player, "worlds_setitem_hand_empty");
            return;
        }

        folder.setIcon(XMaterial.matchXMaterial(itemStack));
        Messages.sendMessage(player, "worlds_folder_item_set",
                Map.entry("%folder%", folder.getName())
        );
    }

    private void handleDeletion(Player player, Folder folder) {
        if (!player.hasPermission("buildsystem.folder.delete")) {
            Messages.sendPermissionError(player);
            return;
        }

        boolean hasWorlds = folder.getWorldCount() > 0;
        boolean hasSubFolders = this.folderStorage.getFolders().stream().anyMatch(f -> Objects.equals(f.getParent(), folder));
        if (hasWorlds || hasSubFolders) {
            Messages.sendMessage(player, "worlds_folder_not_empty",
                    Map.entry("%folder%", folder.getName())
            );
            return;
        }

        this.folderStorage.removeFolder(folder);
        Messages.sendMessage(player, "worlds_folder_deleted",
                Map.entry("%folder%", folder.getName())
        );
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.FOLDER;
    }
}
