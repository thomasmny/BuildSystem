/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jetbrains.annotations.ApiStatus;

/**
 * Called after a {@link BuildWorld} has unloaded.
 */
public class BuildWorldPostUnloadEvent extends BuildWorldEvent {

    @ApiStatus.Internal
    public BuildWorldPostUnloadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}