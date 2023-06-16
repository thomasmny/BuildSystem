/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;

/**
 * Called after a {@link BuildWorld} has loaded.
 */
public class BuildWorldPostLoadEvent extends BuildWorldEvent {

    public BuildWorldPostLoadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}