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
public class ConfigCommand implements CommandExecutor {
    private final BuildSystem plugin;

    public ConfigCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("config").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("buildsystem.config")) {
            plugin.sendPermissionMessage(sender);
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.getString("config_usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "rl":
            case "reload":
                plugin.reloadConfig();
                plugin.reloadConfigData(true);
                sender.sendMessage(plugin.getString("config_reloaded"));
                break;
            default:
                sender.sendMessage(plugin.getString("config_usage"));
                break;
        }

        return true;
    }
}
