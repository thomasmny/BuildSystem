package com.eintosti.buildsystem.event.world;

import com.eintosti.buildsystem.world.BuildWorld;

/**
 * @author einTosti
 */
public class BuildWorldUnloadEvent extends BuildWorldEvent {

    public BuildWorldUnloadEvent(BuildWorld buildWorld) {
        super(buildWorld);

    }
}