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
public class SpeedTabComplete implements TabCompleter {

    public SpeedTabComplete(BuildSystem plugin) {
        plugin.getCommand("Speed").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        switch (label.toLowerCase()) {
            case "speed":
            case "s":
                for (int i = 1; i <= 5; i++) {
                    arrayList.add(String.valueOf(i));
                }
                break;
        }
        return arrayList;
    }
}
