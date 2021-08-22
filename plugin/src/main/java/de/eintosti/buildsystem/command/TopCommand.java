package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class TopCommand implements CommandExecutor {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public TopCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("top").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.top")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length == 0) {
            sendToTop(player);
        } else {
            player.sendMessage(plugin.getString("top_usage"));
        }
        return true;
    }

    private void sendToTop(Player player) {
        World bukkitWorld = player.getWorld();
        Location playerLocation = player.getLocation();
        Location blockLocation = null;

        for (int y = bukkitWorld.getMaxHeight(); y > 0; y--) {
            Block block = bukkitWorld.getBlockAt(playerLocation.getBlockX(), y, playerLocation.getBlockZ());
            if (worldManager.isSafeLocation(block.getLocation()) && y > playerLocation.getY()) {
                blockLocation = block.getLocation();
                break;
            }
        }

        if (blockLocation != null && !Objects.equals(blockLocation.getBlock(), playerLocation.getBlock())) {
            player.teleport(blockLocation.add(0.5, 0, 0.5));
            player.sendMessage(plugin.getString("top_teleported"));
        } else {
            player.sendMessage(plugin.getString("top_failed"));
        }
    }
}
