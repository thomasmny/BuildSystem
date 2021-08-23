package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class SpawnTabComplete extends ArgumentSorter implements TabCompleter {

    public SpawnTabComplete(BuildSystem plugin) {
        plugin.getCommand("Spawn").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (!(sender instanceof Player)) return arrayList;
        Player player = (Player) sender;

        if (player.hasPermission("buildsystem.spawn")) {
            for (Argument argument : Argument.values()) {
                String argumentName = argument.name();
                addArgument(args[0], argumentName, arrayList);
            }
        }

        return arrayList;
    }

    private enum Argument {
        set, remove
    }
}

