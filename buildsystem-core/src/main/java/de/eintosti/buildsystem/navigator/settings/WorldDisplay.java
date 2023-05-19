/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator.settings;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class WorldDisplay implements ConfigurationSerializable {

    private WorldSort worldSort;
    private final WorldFilter worldFilter;

    public WorldDisplay() {
        this.worldSort = WorldSort.NAME_A_TO_Z;
        this.worldFilter = new WorldFilter();
    }

    public WorldDisplay(WorldSort worldSort, WorldFilter worldFilter) {
        this.worldSort = worldSort;
        this.worldFilter = worldFilter;
    }

    public WorldSort getWorldSort() {
        return worldSort;
    }

    public void setWorldSort(WorldSort worldSort) {
        this.worldSort = worldSort;
    }

    public WorldFilter getWorldFilter() {
        return worldFilter;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> display = new HashMap<>();
        display.put("sort", getWorldSort().toString());
        display.put("filter", getWorldFilter().serialize());
        return display;
    }
}