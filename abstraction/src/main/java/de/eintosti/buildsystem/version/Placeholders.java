package de.eintosti.buildsystem.version;

import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public abstract class Placeholders {

    public String injectPlaceholders(String originalString, Player player, String... information) {
        return originalString.replace("%world%", player.getWorld().getName())
                .replace("%status%", information[0])
                .replace("%permission%", information[1])
                .replace("%project%", information[2])
                .replace("%creator%", information[3])
                .replace("%creation%", information[4]);
    }
}
