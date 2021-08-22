package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.Generator;
import de.eintosti.buildsystem.object.world.World;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class WorldsTabComplete extends ArgumentSorter implements TabCompleter {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public WorldsTabComplete(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("Worlds").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (label.equalsIgnoreCase("worlds")) {
            switch (args.length) {
                case 1: {
                    for (Argument argument : Argument.values()) {
                        String command = argument.getCommand();
                        addArgument(args[0], command, arrayList);
                    }
                    return arrayList;
                }
                case 2: {
                    switch (args[0].toLowerCase()) {
                        case "builders":
                        case "delete":
                        case "edit":
                        case "info":
                        case "rename":
                        case "setcreator":
                        case "setitem":
                        case "setpermission":
                        case "setproject":
                        case "setstatus":
                        case "tp":
                            worldManager.getWorlds().forEach(world -> {
                                if (sender.hasPermission(world.getPermission()) || world.getPermission().equalsIgnoreCase("-")) {
                                    String worldName = world.getName();
                                    addArgument(args[1], worldName, arrayList);
                                }
                            });
                            break;
                        case "addbuilder":
                        case "removebuilder":
                            if (!(sender instanceof Player)) return arrayList;
                            Player player = (Player) sender;

                            worldManager.getWorlds().forEach(world -> {
                                if (player.hasPermission("buildsystem.admin") || (world.getCreatorId() != null && world.getCreatorId().equals(player.getUniqueId()))) {
                                    String worldName = world.getName();
                                    addArgument(args[1], worldName, arrayList);
                                }
                            });
                            break;
                        case "import":
                            File worldContainer = Bukkit.getWorldContainer();
                            String[] directories = worldContainer.list((dir, name) -> {
                                for (String charString : name.split("")) {
                                    if (charString.matches("[^A-Za-z0-9/_-]")) {
                                        return false;
                                    }
                                }

                                File worldFolder = new File(dir, name);
                                if (!worldFolder.isDirectory()) return false;

                                File levelFile = new File(dir + File.separator + name + File.separator + "level.dat");
                                if (!levelFile.exists()) return false;

                                World world = worldManager.getWorld(name);
                                return world == null;
                            });

                            if (directories == null || directories.length == 0) return arrayList;
                            for (String projectName : directories) {
                                addArgument(args[1], projectName, arrayList);
                            }
                            break;
                    }
                    return arrayList;
                }
                case 3: {
                    if (args[0].equalsIgnoreCase("import")) {
                        if (args[1].equalsIgnoreCase(" ")) {
                            return arrayList;
                        }
                        arrayList.add("-g");
                        return arrayList;
                    }
                }
                case 4: {
                    if (!args[2].equalsIgnoreCase("-g")) return arrayList;
                    for (Generator value : new Generator[]{Generator.NORMAL, Generator.FLAT, Generator.VOID}) {
                        String valueName = value.name();
                        addArgument(args[3], valueName, arrayList);
                    }
                    return arrayList;
                }
            }
        }
        return arrayList;
    }

    private enum Argument {
        ADD_BUILDER("addBuilder"),
        BUILDERS("builders"),
        DELETE("delete"),
        EDIT("edit"),
        IMPORT("import"),
        IMPORT_ALL("importAll"),
        INFO("info"),
        ITEM("item"),
        REMOVE_BUILDER("removeBuilder"),
        RENAME("rename"),
        SET_CREATOR("setCreator"),
        SET_ITEM("setItem"),
        SET_PERMISSION("setPermission"),
        SET_PROJECT("setProject"),
        SET_STATUS("setStatus"),
        SET_SPAWN("setSpawn"),
        REMOVE_SPAWN("removeSpawn"),
        TP("tp");

        private final String command;

        Argument(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }
}
