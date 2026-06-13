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
package de.eintosti.buildsystem.api.world.access;

import de.eintosti.buildsystem.api.world.data.WorldData;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;

/**
 * A protectable boolean world setting that gates whether a player may modify the world in a particular way.
 *
 * @since TODO
 */
@NullMarked
public enum WorldSetting {

    /**
     * Whether blocks may be broken in the world.
     */
    BLOCK_BREAKING("buildsystem.bypass.settings", WorldData::isBlockBreaking),

    /**
     * Whether blocks may be placed in the world.
     */
    BLOCK_PLACEMENT("buildsystem.bypass.settings", WorldData::isBlockPlacement),

    /**
     * Whether blocks may be interacted with in the world.
     */
    BLOCK_INTERACTIONS("buildsystem.bypass.settings", WorldData::isBlockInteractions);

    private final String bypassPermission;
    private final Predicate<WorldData> enabled;

    WorldSetting(String bypassPermission, Predicate<WorldData> enabled) {
        this.bypassPermission = bypassPermission;
        this.enabled = enabled;
    }

    /**
     * Gets the permission node that lets a player modify the world even when this setting would otherwise deny it.
     *
     * @return The bypass permission node
     */
    public String getBypassPermission() {
        return bypassPermission;
    }

    /**
     * Gets whether this setting is currently enabled for the given world data.
     *
     * @param data The world data to read
     * @return {@code true} if the setting is enabled, otherwise {@code false}
     */
    public boolean isEnabled(WorldData data) {
        return enabled.test(data);
    }
}
