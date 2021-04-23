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
public class BlocksCommand implements CommandExecutor {
    private final BuildSystem plugin;

    public BlocksCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("blocks").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.blocks")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        plugin.getBlocksInventory().openInventory(player);
        return true;
    }
}
