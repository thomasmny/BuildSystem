package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.listener.PlayerTeleportListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class BackCommand implements CommandExecutor {
    private final BuildSystem plugin;

    public BackCommand(BuildSystem plugin) {
        this.plugin = plugin;
        Bukkit.getPluginCommand("back").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.back")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length == 0) {
            teleportBack(player);
        } else {
            player.sendMessage(plugin.getString("back_usage"));
        }
        return true;
    }

    private void teleportBack(Player player) {
        PlayerTeleportListener playerTeleportListener = plugin.getPlayerTeleportListener();
        Location previousLocation = playerTeleportListener.getPreviousLocation(player);

        if (previousLocation == null) {
            player.sendMessage(plugin.getString("back_failed"));
            return;
        }
        player.teleport(previousLocation);
        player.sendMessage(plugin.getString("back_teleported"));
        playerTeleportListener.resetPreviousLocation(player);
    }
}
