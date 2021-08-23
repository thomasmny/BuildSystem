package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class GamemodeTabComplete extends ArgumentSorter implements TabCompleter {

    public GamemodeTabComplete(BuildSystem plugin) {
        plugin.getCommand("gamemode").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (!(sender instanceof Player)) return arrayList;
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.gamemode")) {
            return arrayList;
        }

        switch (label.toLowerCase()) {
            case "gamemode":
            case "gm":
                if (args.length == 1) {
                    for (GameMode gameMode : GameMode.values()) {
                        String gameModeName = gameMode.name();
                        addArgument(args[0], gameModeName, arrayList);
                    }
                } else if (args.length == 2) {
                    if (!player.hasPermission("buildsystem.gamemode.others")) {
                        return arrayList;
                    }

                    switch (args[0].toLowerCase()) {
                        case "survival":
                        case "s":
                        case "0":
                        case "creative":
                        case "c":
                        case "1":
                        case "adventure":
                        case "a":
                        case "2":
                        case "spectator":
                        case "sp":
                        case "3":
                            Bukkit.getOnlinePlayers().forEach(pl -> addArgument(args[1], pl.getName(), arrayList));
                            break;
                    }
                }
                break;
        }

        return arrayList;
    }
}
