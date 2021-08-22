package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * @author einTosti
 */
public class EntitySpawnListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public EntitySpawnListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        World bukkitWorld = event.getLocation().getWorld();

        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null) return;
        if (buildWorld.isMobAI()) return;

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (plugin.getManageEntityAI() != null) {
                plugin.getManageEntityAI().setAI(livingEntity, false);
            } else {
                livingEntity.setAI(false);
            }
        }
    }
}
