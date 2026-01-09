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
package de.eintosti.buildsystem.util.inventory;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jspecify.annotations.NullMarked;

/**
 * Represents an inventory holder specifically for a {@link BuildWorld}, extending {@link BuildSystemHolder}. This allows inventories to be associated with a particular build
 * world.
 */
@NullMarked
public class BuildWorldHolder extends BuildSystemHolder {

    private final BuildWorld buildWorld;

    /**
     * Initializes a new {@link BuildWorldHolder} with the specified {@link BuildWorld}, size, and title.
     *
     * @param size  The size of the inventory.
     * @param title The title of the inventory.
     */
    public BuildWorldHolder(BuildWorld buildWorld, int size, String title) {
        super(size, title);
        this.buildWorld = buildWorld;
    }

    /**
     * Returns the {@link BuildWorld} associated with this inventory holder.
     *
     * @return The associated {@link BuildWorld} instance.
     */
    public BuildWorld getBuildWorld() {
        return buildWorld;
    }
}
