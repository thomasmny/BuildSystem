package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class ExplosionsCommand implements CommandExecutor {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public ExplosionsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("explosions").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.explosions")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                toggleExplosions(player, player.getWorld());
                break;
            case 1:
                toggleExplosions(player, Bukkit.getWorld(args[0]));
                break;
            default:
                player.sendMessage(plugin.getString("explosions_usage"));
                break;
        }
        return true;
    }

    private void toggleExplosions(Player player, World bukkitWorld) {
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("explosions_unknown_world"));
            return;
        }
        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());

        if (buildWorld == null) {
            player.sendMessage(plugin.getString("explosions_world_not_imported"));
            return;
        }

        if (!buildWorld.isExplosions()) {
            buildWorld.setExplosions(true);
            player.sendMessage(plugin.getString("explosions_activated").replace("%world%", buildWorld.getName()));
        } else {
            buildWorld.setExplosions(false);
            player.sendMessage(plugin.getString("explosions_deactivated").replace("%world%", buildWorld.getName()));
        }
    }
}
