package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.world.BuildWorld;
import org.bukkit.event.Cancellable;

/**
 * Called when a {@link BuildWorld} is unloaded.
 */
public class BuildWorldUnloadEvent extends BuildWorldEvent implements Cancellable {

    private boolean cancelled = false;

    public BuildWorldUnloadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}