package de.eintosti.buildsystem.event;

import de.eintosti.buildsystem.event.world.BuildWorldManipulationEvent;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class BuildWorldManipulationEventDispatcher {
    private final WorldManager worldManager;

    public BuildWorldManipulationEventDispatcher(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    /**
     * @param player      player who manipulated.
     * @param parentEvent the event which is considered as manipultion
     * @return true if event has been dispatched
     */
    public boolean dispatchIfPlayerInBuildWorld(Player player, Cancellable parentEvent) {
        if (parentEvent.isCancelled()) {
            return false;
        }
        BuildWorld world = worldManager.getBuildWorld(player.getWorld().getName());
        if (world == null) {
            return false;
        }
        Bukkit.getPluginManager().callEvent(new BuildWorldManipulationEvent(parentEvent, player, world));
        return true;
    }


}
