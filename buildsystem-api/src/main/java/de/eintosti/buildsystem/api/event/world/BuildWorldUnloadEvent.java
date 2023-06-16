/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;
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