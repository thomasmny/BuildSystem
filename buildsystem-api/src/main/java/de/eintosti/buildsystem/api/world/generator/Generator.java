/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world.generator;

import de.eintosti.buildsystem.api.world.data.WorldType;

public enum Generator {
    NORMAL(WorldType.NORMAL),
    FLAT(WorldType.FLAT),
    VOID(WorldType.VOID),
    CUSTOM(WorldType.IMPORTED);

    private final WorldType worldType;

    Generator(WorldType worldType) {
        this.worldType = worldType;
    }

    public WorldType getWorldType() {
        return worldType;
    }
}