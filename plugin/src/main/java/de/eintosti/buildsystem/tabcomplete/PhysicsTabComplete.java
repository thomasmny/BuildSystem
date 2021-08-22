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
public class PhysicsTabComplete extends ArgumentSorter implements TabCompleter {
    private final WorldManager worldManager;

    public PhysicsTabComplete(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("Physics").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (args.length == 1) {
            worldManager.getBuildWorlds().forEach(world -> {
                if (sender.hasPermission("buildsystem.physics")) {
                    String worldName = world.getName();
                    addArgument(args[0], worldName, arrayList);
                }
            });
            return arrayList;
        }
        return arrayList;
    }
}
