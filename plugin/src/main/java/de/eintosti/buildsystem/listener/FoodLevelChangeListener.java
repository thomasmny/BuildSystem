package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * @author einTosti
 */
public class FoodLevelChangeListener implements Listener {
    private final WorldManager worldManager;

    public FoodLevelChangeListener(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        World bukkitWorld = player.getWorld();
        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());

        if (buildWorld == null) return;
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            event.setCancelled(true);
        }
    }
}
