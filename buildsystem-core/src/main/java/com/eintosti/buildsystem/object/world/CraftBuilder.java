/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.world;

import com.eintosti.buildsystem.api.world.Builder;

import java.util.UUID;

/**
 * @author einTosti
 */
public class CraftBuilder implements Builder {

    private final UUID uuid;
    private final String name;

    public CraftBuilder(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return uuid.toString() + "," + name;
    }
}