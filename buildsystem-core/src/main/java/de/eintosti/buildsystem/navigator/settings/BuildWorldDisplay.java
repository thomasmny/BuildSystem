/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator.settings;

import de.eintosti.buildsystem.api.settings.WorldDisplay;
import de.eintosti.buildsystem.api.settings.WorldSort;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BuildWorldDisplay implements WorldDisplay, ConfigurationSerializable {

    private WorldSort worldSort;
    private final BuildWorldFilter worldFilter;

    public BuildWorldDisplay() {
        this.worldSort = WorldSort.NAME_A_TO_Z;
        this.worldFilter = new BuildWorldFilter();
    }

    public BuildWorldDisplay(WorldSort worldSort, BuildWorldFilter worldFilter) {
        this.worldSort = worldSort;
        this.worldFilter = worldFilter;
    }

    @Override
    public WorldSort getWorldSort() {
        return worldSort;
    }

    @Override
    public void setWorldSort(WorldSort worldSort) {
        this.worldSort = worldSort;
    }

    @Override
    public BuildWorldFilter getWorldFilter() {
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