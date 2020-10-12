package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class NoAICommand implements CommandExecutor {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public NoAICommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        Bukkit.getPluginCommand("noai").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.noai")) {
            plugin.sendPermissionMessage(player);
            return true;
        }
        switch (args.length) {
            case 0:
                toggleAI(player, player.getWorld());
                break;
            case 1:
                toggleAI(player, Bukkit.getWorld(args[0]));
                break;
            default:
                player.sendMessage(plugin.getString("noai_usage"));
                break;
        }
        return true;
    }

    private void toggleAI(Player player, org.bukkit.World bukkitWorld) {
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("noai_unknown_world"));
            return;
        }
        World world = worldManager.getWorld(bukkitWorld.getName());

        if (world == null) {
            player.sendMessage(plugin.getString("noai_world_not_imported"));
            return;
        }
        if (!world.isMobAI()) {
            world.setMobAI(true);
            player.sendMessage(plugin.getString("noai_deactivated").replace("%world%", world.getName()));
        } else {
            world.setMobAI(false);
            player.sendMessage(plugin.getString("noai_activated").replace("%world%", world.getName()));
        }
        for (Entity entity : bukkitWorld.getEntities()) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (plugin.getManageEntityAI() != null) {
                    plugin.getManageEntityAI().setAI(livingEntity, world.isMobAI());
                } else {
                    livingEntity.setAI(world.isMobAI());
                }
            }
        }
    }
}
