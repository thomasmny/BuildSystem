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
public class PhysicsCommand implements CommandExecutor {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public PhysicsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("physics").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.physics")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                togglePhysics(player, player.getWorld());
                break;
            case 1:
                if (args[0].equalsIgnoreCase("all") && worldManager.getBuildWorld("all") == null) {
                    worldManager.getBuildWorlds().forEach(world -> world.setPhysics(true));
                    player.sendMessage(plugin.getString("physics_activated_all"));
                } else {
                    togglePhysics(player, Bukkit.getWorld(args[0]));
                }
                break;
            default:
                player.sendMessage(plugin.getString("physics_usage"));
                break;
        }
        return true;
    }

    private void togglePhysics(Player player, World bukkitWorld) {
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("physics_unknown_world"));
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("physics_world_not_imported"));
            return;
        }

        if (!buildWorld.isPhysics()) {
            buildWorld.setPhysics(true);
            player.sendMessage(plugin.getString("physics_activated").replace("%world%", buildWorld.getName()));
        } else {
            buildWorld.setPhysics(false);
            player.sendMessage(plugin.getString("physics_deactivated").replace("%world%", buildWorld.getName()));
        }
    }
}
