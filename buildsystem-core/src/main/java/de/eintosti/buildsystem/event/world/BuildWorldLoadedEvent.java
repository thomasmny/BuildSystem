package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.world.BuildWorld;

/**
 * Called after a {@link BuildWorld} has loaded.
 */
public class BuildWorldLoadedEvent extends BuildWorldEvent {

    public BuildWorldLoadedEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}