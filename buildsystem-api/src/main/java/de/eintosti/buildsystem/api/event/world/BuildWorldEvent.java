/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@link BuildWorld} related event.
 */
public class BuildWorldEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final BuildWorld buildWorld;

    public BuildWorldEvent(BuildWorld buildWorld) {
        this.buildWorld = buildWorld;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public BuildWorld getBuildWorld() {
        return buildWorld;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}