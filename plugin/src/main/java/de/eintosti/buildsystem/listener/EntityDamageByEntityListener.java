package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
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

        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) return;

        disableArchivedWorlds(buildWorld, player, event);
        checkWorldSettings(buildWorld, player, event);
        checkBuilders(buildWorld, player, event);
    }

    private void disableArchivedWorlds(BuildWorld buildWorld, Player player, EntityDamageByEntityEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(BuildWorld buildWorld, Player player, EntityDamageByEntityEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!buildWorld.isBlockInteractions() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, EntityDamageByEntityEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return;
        }
        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
        }
    }
}
