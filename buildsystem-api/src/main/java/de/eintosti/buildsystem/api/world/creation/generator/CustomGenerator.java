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
package de.eintosti.buildsystem.api.world.creation.generator;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a custom chunk generator for a {@link World} which is provided by an external plugin.
 *
 * @since 3.0.0
 */
@NullMarked
public interface CustomGenerator {

    /**
     * Gets the name plugin providing the chunk generator.
     *
     * @return The name of the plugin
     */
    String pluginName();

    /**
     * Gets the name of the chunk generator.
     *
     * @return The name of the chunk generator
     */
    String chunkGeneratorName();

    /**
     * Gets the chunk generator.
     *
     * @return The chunk generator
     */
    ChunkGenerator chunkGenerator();
}