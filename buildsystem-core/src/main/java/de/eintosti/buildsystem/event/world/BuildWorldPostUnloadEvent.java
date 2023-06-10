package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.world.BuildWorld;

/**
 * Called after a {@link BuildWorld} has unloaded.
 */
public class BuildWorldPostUnloadEvent extends BuildWorldEvent {

    public BuildWorldPostUnloadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}