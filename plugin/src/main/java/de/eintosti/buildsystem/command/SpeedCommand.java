package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class SpeedCommand implements CommandExecutor {
    private final BuildSystem plugin;

    public SpeedCommand(BuildSystem plugin) {
        this.plugin = plugin;
        Bukkit.getPluginCommand("speed").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.speed")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length == 0) {
            plugin.getSpeedInventory().openInventory(player);
            XSound.BLOCK_CHEST_OPEN.play(player);
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "1":
                    setSpeed(player, 0.2f, args);
                    break;
                case "2":
                    setSpeed(player, 0.4f, args);
                    break;
                case "3":
                    setSpeed(player, 0.6f, args);
                    break;
                case "4":
                    setSpeed(player, 0.8f, args);
                    break;
                case "5":
                    setSpeed(player, 1.0f, args);
                    break;
                default:
                    player.sendMessage(plugin.getString("speed_usage"));
                    break;
            }
        } else {
            player.sendMessage(plugin.getString("speed_usage"));
        }
        return true;
    }

    private void setSpeed(Player player, float speed, String[] args) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            player.sendMessage(plugin.getString("speed_set_flying").replace("%speed%", args[0]));
        } else {
            player.setWalkSpeed(speed);
            player.sendMessage(plugin.getString("speed_set_walking").replace("%speed%", args[0]));
        }
    }
}
