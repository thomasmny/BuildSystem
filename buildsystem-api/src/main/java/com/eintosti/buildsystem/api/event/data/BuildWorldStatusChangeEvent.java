/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.event.data;

import com.eintosti.buildsystem.api.world.BuildWorld;
import com.eintosti.buildsystem.api.world.WorldStatus;

/**
 * @author einTosti
 */
public class BuildWorldStatusChangeEvent extends BuildWorldDataChangeEvent<WorldStatus> {

    private final Reason reason;

    public BuildWorldStatusChangeEvent(BuildWorld buildWorld, WorldStatus oldStatus, WorldStatus newStatus, Reason reason) {
        super(buildWorld, oldStatus, newStatus);
        this.reason = reason;
    }

    public WorldStatus getOldStatus() {
        return getOldData();
    }

    public WorldStatus getNewStatus() {
        return getNewData();
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        BLOCK_BREAK,
        BLOCK_PLACE,
        COMMAND,
        PLUGIN,
    }
}
