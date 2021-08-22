package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class TimeTabComplete extends ArgumentSorter implements TabCompleter {
    private final WorldManager worldManager;

    public TimeTabComplete(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("Day").setTabCompleter(this);
        plugin.getCommand("Night").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        switch (label.toLowerCase()) {
            case "day":
                worldManager.getBuildWorlds().forEach(world -> {
                    if (sender.hasPermission("buildsystem.day")) {
                        String worldName = world.getName();
                        addArgument(args[0], worldName, arrayList);
                    }
                });
                return arrayList;
            case "night":
                worldManager.getBuildWorlds().forEach(world -> {
                    if (sender.hasPermission("buildsystem.night")) {
                        String worldName = world.getName();
                        addArgument(args[0], worldName, arrayList);
                    }
                });
                return arrayList;
        }
        return arrayList;
    }
}
