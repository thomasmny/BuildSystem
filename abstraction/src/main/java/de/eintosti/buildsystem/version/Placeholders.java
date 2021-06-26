package de.eintosti.buildsystem.version;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public abstract class Placeholders {

    public String injectPlaceholders(String originalString, Player player, String... information) {
        return originalString
                .replace("%world%", player.getWorld().getName())
                .replace("%status%", information[0])
                .replace("%permission%", information[1])
                .replace("%project%", information[2])
                .replace("%creator%", information[3])
                .replace("%creation%", information[4]);
    }

    public List<String> replacedList(List<String> oldList, Player player, String... information) {
        List<String> list = new ArrayList<>();
        oldList.forEach(body -> list.add(ChatColor.translateAlternateColorCodes('&', injectPlaceholders(body, player, information))));
        return list;
    }
}
