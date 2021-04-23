package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class SettingsCommand implements CommandExecutor {
    private final BuildSystem plugin;

    public SettingsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("settings").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.settings")) {
            plugin.sendPermissionMessage(player);
            return true;
        }
        plugin.getSettingsInventory().openInventory(player);
        return true;
    }
}
