package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/**
 * @author einTosti
 */
public class SignChangeListener implements Listener {

    public SignChangeListener(BuildSystem plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("buildsystem.color.sign")) return;

        for (int i = 0; i < event.getLines().length; i++) {
            event.setLine(i, ChatColor.translateAlternateColorCodes('&', event.getLine(i)));
        }
    }
}
