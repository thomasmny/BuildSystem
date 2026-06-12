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
import com.google.common.collect.Lists;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ArchiveSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BackupsSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BuildersSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.DeleteSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.EditSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.FolderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.HelpSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportAllSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.InfoSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.PrivateSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.PublicSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RenameSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetCreatorSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetStatusSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.TeleportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.UnimportSubCommand;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.navigator.inventory.NavigatorInventory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldsCommand extends CommandBase {

    private final WorldStorage worldStorage;
    private final FolderStorage folderStorage;

    public WorldsCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
        WorldService worldService = plugin.getWorldService();
        this.worldStorage = worldService.getWorldStorage();
        this.folderStorage = worldService.getFolderStorage();
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (!requirePermission(player, "buildsystem.navigator")) {
                return;
            }

            new NavigatorInventory(plugin).openInventory(player);
            XSound.BLOCK_CHEST_OPEN.play(player);
            return;
        }

        WorldsArgument argument = WorldsArgument.matchArgument(args[0]);
        if (argument == null) {
            messages.sendMessage(player, "worlds_unknown_command");
            return;
        }

        // Most commands use the structure /worlds <argument> <world> <...> which is why we assume that args[1] is the world name
        // Make sure to change if this is not the case for any specific command
        String worldName = args.length >= 2 ? args[1] : player.getWorld().getName();

        SubCommand subCommand = switch (argument) {
            case ARCHIVE -> new ArchiveSubCommand(plugin);
            case ADD_BUILDER -> new AddBuilderSubCommand(plugin, player.getWorld().getName());
            case BACKUP -> new BackupsSubCommand(plugin);
            case BUILDERS -> new BuildersSubCommand(plugin, worldName);
            case DELETE -> new DeleteSubCommand(plugin, worldName);
            case EDIT -> new EditSubCommand(plugin, worldName);
            case FOLDER -> new FolderSubCommand(plugin);
            case HELP -> new HelpSubCommand();
            case IMPORT_ALL -> new ImportAllSubCommand(plugin);
            case IMPORT -> new ImportSubCommand(plugin, worldName);
            case INFO -> new InfoSubCommand(plugin, worldName);
            case ITEM -> new ItemSubCommand();
            case PRIVATE -> new PrivateSubCommand(plugin);
            case PUBLIC -> new PublicSubCommand(plugin);
            case REMOVE_BUILDER -> new RemoveBuilderSubCommand(plugin, player.getWorld().getName());
            case REMOVE_SPAWN -> new RemoveSpawnSubCommand(plugin);
            case RENAME -> new RenameSubCommand(plugin, worldName);
            case SET_CREATOR -> new SetCreatorSubCommand(plugin, worldName);
            case SET_ITEM -> new SetItemSubCommand(plugin, worldName);
            case SET_PERMISSION -> new SetPermissionSubCommand(plugin, worldName);
            case SET_PROJECT -> new SetProjectSubCommand(plugin, worldName);
            case SET_SPAWN -> new SetSpawnSubCommand(plugin);
            case SET_STATUS -> new SetStatusSubCommand(plugin, worldName);
            case TP -> new TeleportSubCommand(plugin);
            case UNIMPORT -> new UnimportSubCommand(plugin, worldName);
        };
        subCommand.execute(player, args);
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        switch (args.length) {
            case 1: {
                for (WorldsArgument argument : WorldsArgument.values()) {
                    String command = argument.getName();
                    String permission = argument.getPermission();
                    if (player.hasPermission(permission)) {
                        addArgument(args[0], command, arrayList);
                    }
                }
                return arrayList;
            }

            case 2: {
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "builders":
                    case "edit":
                    case "info":
                    case "rename":
                    case "setcreator":
                    case "setitem":
                    case "setpermission":
                    case "setproject":
                    case "setstatus":
                    case "tp":
                    case "unimport": {
                        worldStorage.getBuildWorlds().stream()
                                .filter(world -> {
                                    String permission = world.getData().permission().get();
                                    return player.hasPermission(permission) || permission.equalsIgnoreCase("-");
                                })
                                .filter(world -> world.getPermissions().canPerformCommand(player, "buildsystem." + args[0].toLowerCase(Locale.ROOT)))
                                .forEach(world -> addArgument(args[1], world.getName(), arrayList));
                        break;
                    }

                    case "backup": {
                        if (player.hasPermission(WorldsArgument.BACKUP.getPermission() + ".create")) {
                            addArgument(args[1], "create", arrayList);
                        }
                        break;
                    }

                    case "delete": {
                        worldStorage.getBuildWorlds().stream()
                                .filter(world -> world.getPermissions().canPerformCommand(player, "buildsystem." + args[0].toLowerCase(Locale.ROOT)))
                                .forEach(world -> addArgument(args[1], world.getName(), arrayList));
                        break;
                    }

                    case "addbuilder": {
                        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
                        if (buildWorld == null) {
                            break;
                        }
                        Builders builders = buildWorld.getBuilders();
                        Bukkit.getOnlinePlayers().stream()
                                .filter(pl -> !builders.isBuilder(pl) && !builders.isCreator(pl))
                                .forEach(pl -> addArgument(args[1], pl.getName(), arrayList));
                        break;
                    }

                    case "removebuilder": {
                        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
                        if (buildWorld == null) {
                            break;
                        }
                        Builders builders = buildWorld.getBuilders();
                        if (builders.isCreator(player)) {
                            builders.getBuilderNames().forEach(builderName -> addArgument(args[1], builderName, arrayList));
                        }
                        break;
                    }

                    case "import": {
                        String[] directories = Bukkit.getWorldContainer().list((dir, name) -> {
                            if (StringCleaner.hasInvalidNameCharacters(name)) {
                                return false;
                            }

                            File worldFolder = new File(dir, name);
                            if (!worldFolder.isDirectory()) {
                                return false;
                            }

                            if (!new File(worldFolder, "level.dat").exists()) {
                                return false;
                            }

                            return !worldStorage.worldExists(name);
                        });

                        if (directories == null || directories.length == 0) {
                            return arrayList;
                        }

                        for (String projectName : directories) {
                            addArgument(args[1], projectName, arrayList);
                        }
                        break;
                    }

                    case "folder": {
                        folderStorage.getFolders().stream()
                                .map(Displayable::getName)
                                .forEach(folderName -> addArgument(args[1], folderName, arrayList));
                        break;
                    }
                }
                return arrayList;
            }

            default:
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "import": {
                        Map<String, List<String>> arguments = Map.of(
                                "-g", Arrays.stream(Generator.values()).filter(generator -> generator != Generator.CUSTOM).map(Enum::name).toList(),
                                "-c", List.of(),
                                "-t", Arrays.stream(BuildWorldType.values()).map(Enum::name).toList()
                        );

                        if (args.length % 2 == 1) {
                            arguments.keySet().stream()
                                    .filter(key -> !Lists.newArrayList(args).contains(key))
                                    .forEach(argument -> addArgument(args[args.length - 1], argument, arrayList));
                        } else {
                            List<String> values = arguments.get(args[args.length - 2]);
                            if (values != null) {
                                for (String argument : values) {
                                    addArgument(args[args.length - 1], argument, arrayList);
                                }
                            }
                        }
                        break;
                    }

                    case "folder": {
                        switch (args.length) {
                            case 3:
                                Map<String, String> subCommands = Map.of(
                                        "add", "buildsystem.folder.add",
                                        "remove", "buildsystem.folder.remove",
                                        "delete", "buildsystem.folder.delete",
                                        "setPermission", "buildsystem.folder.setpermission",
                                        "setProject", "buildsystem.folder.setproject",
                                        "setItem", "buildsystem.folder.setitem"
                                );
                                subCommands.entrySet().stream()
                                        .filter(entry -> player.hasPermission(entry.getKey()))
                                        .forEach(entry -> addArgument(args[2], entry.getKey(), arrayList));
                                break;
                            case 4:
                                if (!args[2].equalsIgnoreCase("add") && !args[2].equalsIgnoreCase("remove")) {
                                    return arrayList;
                                }

                                Folder folder = folderStorage.getFolder(args[1]);
                                if (folder == null) {
                                    return arrayList;
                                }

                                worldStorage.getBuildWorlds().stream()
                                        .filter(buildWorld -> NavigatorCategory.of(buildWorld) == folder.getCategory())
                                        .filter(buildWorld -> {
                                            if (args[2].equalsIgnoreCase("add")) {
                                                return !buildWorld.isAssignedToFolder();
                                            } else if (args[2].equalsIgnoreCase("remove")) {
                                                return folder.containsWorld(buildWorld);
                                            }
                                            return false;
                                        })
                                        .forEach(world -> addArgument(args[3], world.getName(), arrayList));
                                break;
                        }
                    }
                }
                return arrayList;
        }
    }

    @NullMarked
    public enum WorldsArgument implements Argument {
        ARCHIVE("archive", "buildsystem.navigator"),
        ADD_BUILDER("addBuilder", "buildsystem.addbuilder"),
        BACKUP("backup", "buildsystem.backup"),
        BUILDERS("builders", "buildsystem.builders"),
        DELETE("delete", "buildsystem.delete"),
        EDIT("edit", "buildsystem.edit"),
        FOLDER("folder", "buildsystem.folder"),
        HELP("help", "buildsystem.help.worlds"),
        IMPORT("import", "buildsystem.import"),
        IMPORT_ALL("importAll", "buildsystem.import.all"),
        INFO("info", "buildsystem.info"),
        ITEM("item", "buildsystem.navigator.item"),
        PRIVATE("private", "buildsystem.navigator"),
        PUBLIC("public", "buildsystem.navigator"),
        REMOVE_BUILDER("removeBuilder", "buildsystem.removebuilder"),
        RENAME("rename", "buildsystem.rename"),
        SET_CREATOR("setCreator", "buildsystem.setcreator"),
        SET_ITEM("setItem", "buildsystem.setitem"),
        SET_PERMISSION("setPermission", "buildsystem.setpermission"),
        SET_PROJECT("setProject", "buildsystem.setproject"),
        SET_STATUS("setStatus", "buildsystem.setstatus"),
        SET_SPAWN("setSpawn", "buildsystem.setspawn"),
        REMOVE_SPAWN("removeSpawn", "buildsystem.removespawn"),
        TP("tp", "buildsystem.worldtp"),
        UNIMPORT("unimport", "buildsystem.unimport");

        private final String command;
        private final String permission;

        WorldsArgument(String command, String permission) {
            this.command = command;
            this.permission = permission;
        }

        @Nullable
        public static WorldsArgument matchArgument(String input) {
            return Arrays.stream(values())
                    .filter(argument -> argument.getName().equalsIgnoreCase(input))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String getName() {
            return command;
        }

        @Override
        public String getPermission() {
            return permission;
        }
    }
}
