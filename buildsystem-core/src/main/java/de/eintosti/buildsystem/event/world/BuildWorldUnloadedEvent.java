package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.world.BuildWorld;

/**
 * Called after a {@link BuildWorld} has unloaded.
 */
public class BuildWorldUnloadedEvent extends BuildWorldEvent {

    public BuildWorldUnloadedEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}