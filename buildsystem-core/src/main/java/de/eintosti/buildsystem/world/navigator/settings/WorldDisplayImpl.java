/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.world.navigator.settings;

import de.eintosti.buildsystem.api.world.navigator.settings.WorldDisplay;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldSort;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldDisplayImpl implements WorldDisplay {

    private final WorldFilterImpl worldFilter;
    private WorldSort worldSort;

    public WorldDisplayImpl() {
        this.worldSort = WorldSort.NAME_A_TO_Z;
        this.worldFilter = new WorldFilterImpl();
    }

    public WorldDisplayImpl(WorldSort worldSort, WorldFilterImpl worldFilter) {
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
    public WorldFilterImpl getWorldFilter() {
        return worldFilter;
    }
}