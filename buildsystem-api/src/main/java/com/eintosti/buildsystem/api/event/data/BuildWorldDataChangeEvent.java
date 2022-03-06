/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.event.data;

import com.eintosti.buildsystem.api.event.BuildWorldEvent;
import com.eintosti.buildsystem.api.world.BuildWorld;

/**
 * @author einTosti
 */
class BuildWorldDataChangeEvent<T> extends BuildWorldEvent {

    private final T oldData;
    private final T newData;

    public BuildWorldDataChangeEvent(BuildWorld buildWorld, T oldData, T newData) {
        super(buildWorld);
        this.oldData = oldData;
        this.newData = newData;
    }

    protected T getOldData() {
        return oldData;
    }

    protected T getNewData() {
        return newData;
    }
}
