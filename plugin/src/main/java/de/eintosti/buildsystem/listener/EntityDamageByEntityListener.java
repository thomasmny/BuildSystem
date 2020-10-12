package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * @author einTosti
 */
public class EntityDamageByEntityListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public EntityDamageByEntityListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        Entity entity = event.getEntity();
        if (!(entity instanceof ArmorStand)) return;

        World world = worldManager.getWorld(player.getWorld().getName());
        if (world == null) return;

        disableArchivedWorlds(world, player, event);
        checkWorldSettings(world, player, event);
        checkBuilders(world, player, event);
    }

    private void disableArchivedWorlds(World world, Player player, EntityDamageByEntityEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (world.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(World world, Player player, EntityDamageByEntityEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!world.isBlockInteractions() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkBuilders(World world, Player player, EntityDamageByEntityEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && world.getCreatorId() != null && world.getCreatorId().equals(player.getUniqueId())) {
            return;
        }
        if (world.isBuilders() && !world.isBuilder(player)) {
            event.setCancelled(true);
        }
    }
}
