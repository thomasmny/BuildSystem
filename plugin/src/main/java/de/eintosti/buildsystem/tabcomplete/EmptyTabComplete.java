package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class EmptyTabComplete implements TabCompleter {

    public EmptyTabComplete(BuildSystem plugin) {
        plugin.getCommand("back").setTabCompleter(this);
        plugin.getCommand("blocks").setTabCompleter(this);
        plugin.getCommand("buildsystem").setTabCompleter(this);
        plugin.getCommand("settings").setTabCompleter(this);
        plugin.getCommand("setup").setTabCompleter(this);
        plugin.getCommand("top").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
