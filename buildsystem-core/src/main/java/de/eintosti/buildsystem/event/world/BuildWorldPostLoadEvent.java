package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.world.BuildWorld;

/**
 * Called after a {@link BuildWorld} has loaded.
 */
public class BuildWorldPostLoadEvent extends BuildWorldEvent {

    public BuildWorldPostLoadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}