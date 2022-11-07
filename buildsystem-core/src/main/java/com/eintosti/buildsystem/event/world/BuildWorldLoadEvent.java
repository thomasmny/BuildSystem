package com.eintosti.buildsystem.event.world;

import com.eintosti.buildsystem.world.BuildWorld;

/**
 * @author einTosti
 */
public class BuildWorldLoadEvent extends BuildWorldEvent {

    public BuildWorldLoadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}