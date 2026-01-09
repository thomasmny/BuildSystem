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

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.World.Environment;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NullMarked;

/**
 * Represents the different types of {@link BuildWorld}s that can be created or managed by the BuildSystem plugin. Each type specifies unique characteristics for world generation
 * and behavior.
 *
 * @since 3.0.0
 */
@NullMarked
public enum BuildWorldType {

    /**
     * A standard world type, equivalent to a default Minecraft overworld with {@link Environment#NORMAL}.
     */
    NORMAL,

    /**
     * A super-flat world, ideal for creative building without terrain obstacles.
     */
    FLAT,

    /**
     * A world type representing the Nether dimension, with {@link Environment#NETHER}.
     */
    NETHER,

    /**
     * A world type representing the End dimension, with {@link Environment#THE_END}.
     */
    END,

    /**
     * An empty world, containing no blocks except for a single platform at spawn.
     */
    VOID,

    /**
     * A world created as an identical copy of an existing template world.
     */
    TEMPLATE,

    /**
     * A world that, by default, can only be modified by its creator.
     */
    PRIVATE,

    /**
     * A world that was not originally created by the BuildSystem plugin but has been imported for management.
     */
    IMPORTED,

    /**
     * A world generated using a custom {@link ChunkGenerator}.
     */
    CUSTOM,

    /**
     * A world whose type could not be determined or is not recognized by the BuildSystem.
     */
    UNKNOWN
}