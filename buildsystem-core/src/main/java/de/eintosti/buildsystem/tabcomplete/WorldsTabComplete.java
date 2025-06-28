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
package de.eintosti.buildsystem.tabcomplete;

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
import de.eintosti.buildsystem.util.StringCleaner;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldsTabComplete extends ArgumentSorter implements TabCompleter {

    private final WorldStorage worldStorage;
    private final FolderStorage folderStorage;

    public WorldsTabComplete(BuildSystemPlugin plugin) {
        WorldService worldService = plugin.getWorldService();
        this.worldStorage = worldService.getWorldStorage();
        this.folderStorage = worldService.getFolderStorage();
        plugin.getCommand("worlds").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (!(sender instanceof Player player)) {
            return arrayList;
        }

        switch (args.length) {
            case 1: {
                for (WorldsArgument argument : WorldsArgument.values()) {
                    String command = argument.getName();
                    String permission = argument.getPermission();

                    if (permission == null || player.hasPermission(permission)) {
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
                                "-g", Arrays.stream(Generator.values()).filter(generator -> generator != Generator.CUSTOM).map(Enum::name).collect(Collectors.toList()),
                                "-c", Lists.newArrayList(),
                                "-t", Arrays.stream(BuildWorldType.values()).map(Enum::name).collect(Collectors.toList())
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
                                        .filter(world -> NavigatorCategory.of(world) == folder.getCategory())
                                        .filter(world -> {
                                            if (args[2].equalsIgnoreCase("add")) {
                                                return !folderStorage.isAssignedToAnyFolder(world);
                                            } else if (args[2].equalsIgnoreCase("remove")) {
                                                return folder.containsWorld(world);
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

    public enum WorldsArgument implements Argument {
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
