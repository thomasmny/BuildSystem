/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world.generator;

import org.bukkit.generator.ChunkGenerator;

public interface CustomGenerator {

    /**
     * Gets the name of the chunk generator.
     *
     * @return The name
     */
    String getName();

    /**
     * Gets the chunk generator.
     *
     * @return The chunk generator
     */
    ChunkGenerator getChunkGenerator();
}