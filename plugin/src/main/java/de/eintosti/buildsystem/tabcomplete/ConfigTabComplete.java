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
public class ConfigTabComplete implements TabCompleter {

    public ConfigTabComplete(BuildSystem plugin) {
        plugin.getCommand("Config").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (sender.hasPermission("buildsystem.config")) {
            arrayList.add("reload");
        }

        return arrayList;
    }
}
