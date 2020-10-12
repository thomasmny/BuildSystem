package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * @author einTosti
 */
public class AsyncPlayerChatListener implements Listener {

    public AsyncPlayerChatListener(BuildSystem plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("buildsystem.color.chat")) return;
        event.setMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
    }
}
