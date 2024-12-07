package de.eintosti.buildsystem.listener;

import com.moulberry.axiom.event.AxiomModifyWorldEvent;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.event.EventDispatcher;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Only register if axiom is available
 */
public class WorldManipulateByAxiomListener implements Listener {

    private final BuildSystem plugin;
    private final EventDispatcher dispatcher;


    /**
     * @param plugin plugin to register.
     */
    public WorldManipulateByAxiomListener(@NotNull BuildSystem plugin) {
        this.plugin = plugin;
        this.dispatcher = new EventDispatcher(plugin.getWorldManager());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler()
    public void onWorldModification(AxiomModifyWorldEvent event) {
        // I don't know if it is possible. Just to be safe.
        if (!event.getPlayer().getWorld().equals(event.getWorld())) {
            event.setCancelled(true);
            throw new IllegalStateException("Player modifies a world in which he is not present! The event got cancelled for safety reasons.");
        }
        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(event.getPlayer(), event);
    }
}
