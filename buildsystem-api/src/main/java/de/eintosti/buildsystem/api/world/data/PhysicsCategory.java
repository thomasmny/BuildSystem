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
package de.eintosti.buildsystem.api.world.data;

import org.jspecify.annotations.NullMarked;

/**
 * A group of world behavior that can individually be re-allowed while {@link WorldDataKey#PHYSICS} is disabled.
 * Each category owns the {@link #key() key} storing its per-world value: {@code true} means the behavior still runs
 * with physics disabled, {@code false} (the default) means it is blocked. While physics is enabled, the values are
 * ignored and vanilla behavior applies.
 *
 * @since 4.0.0
 */
@NullMarked
public enum PhysicsCategory {

    /**
     * Neighbor updates: attached blocks (torches, rails, …) popping off and block shape updates.
     */
    BLOCK_UPDATES("block-updates"),

    /**
     * Fences, gates, glass panes, stairs and walls connecting to adjacent blocks.
     */
    CONNECTIONS("connections"),

    /**
     * Gravity blocks (sand, gravel, …) falling.
     */
    FALLING_BLOCKS("falling-blocks"),

    /**
     * Water and lava flow.
     */
    FLUID_FLOW("fluid-flow"),

    /**
     * Leaves decaying.
     */
    LEAF_DECAY("leaf-decay"),

    /**
     * Crops and plants growing in place.
     */
    GROWTH("growth"),

    /**
     * Grass, vines, fire and similar blocks spreading to other blocks.
     */
    SPREADING("spreading"),

    /**
     * Snow, ice and similar blocks forming.
     */
    BLOCK_FORMING("block-forming"),

    /**
     * Ice and snow melting, corals dying and fire burning out.
     */
    BLOCK_FADING("block-fading");

    private final WorldDataKey<Boolean> key;

    PhysicsCategory(String id) {
        this.key = WorldDataKey.of("physics-exceptions." + id, Boolean.class);
    }

    /**
     * {@return the key storing whether this category still runs while physics is disabled}
     */
    public WorldDataKey<Boolean> key() {
        return key;
    }
}
