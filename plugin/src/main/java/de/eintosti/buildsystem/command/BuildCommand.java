package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class BuildCommand implements CommandExecutor {
    private final BuildSystem plugin;

    public BuildCommand(BuildSystem plugin) {
        this.plugin = plugin;
        Bukkit.getPluginCommand("build").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.build")) {
            plugin.sendPermissionMessage(player);
            return true;
        }
        switch (args.length) {
            case 0:
                toggleBuildMode(player, null, false);
                break;
            case 1:
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getString("build_player_not_found"));
                    return true;
                }
                toggleBuildMode(target, player, true);
                break;
            default:
                player.sendMessage(plugin.getString("build_usage"));
                break;
        }
        return true;
    }

    private void toggleBuildMode(Player target, Player sender, boolean extended) {
        if (plugin.buildPlayers.contains(target.getUniqueId())) {
            plugin.buildPlayers.remove(target.getUniqueId());
            if (plugin.buildPlayerGamemode.containsKey(target.getUniqueId())) {
                target.setGameMode(plugin.buildPlayerGamemode.get(target.getUniqueId()));
                plugin.buildPlayerGamemode.remove(target.getUniqueId());
            }

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (!extended) {
                target.sendMessage(plugin.getString("build_deactivated_self"));
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                sender.sendMessage(plugin.getString("build_deactivated_other_sender").replace("%target%", target.getName()));
                target.sendMessage(plugin.getString("build_deactivated_other_target").replace("%sender%", sender.getName()));
            }
        } else {
            plugin.buildPlayers.add(target.getUniqueId());
            plugin.buildPlayerGamemode.put(target.getUniqueId(), target.getGameMode());
            target.setGameMode(GameMode.CREATIVE);

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (!extended) {
                target.sendMessage(plugin.getString("build_activated_self"));
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                sender.sendMessage(plugin.getString("build_activated_other_sender").replace("%target%", target.getName()));
                target.sendMessage(plugin.getString("build_activated_other_target").replace("%sender%", sender.getName()));
            }
        }
    }
}
