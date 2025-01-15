/*
 * Copyright (c) 2018-2025, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.navigator.settings;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

public class WorldDisplay implements ConfigurationSerializable {

    private final WorldFilter worldFilter;
    private WorldSort worldSort;

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