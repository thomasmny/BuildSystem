/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.generator;

import de.eintosti.buildsystem.api.world.generator.CustomGenerator;
import org.bukkit.generator.ChunkGenerator;

public class CustomGeneratorImpl implements CustomGenerator {

    private final String name;
    private final ChunkGenerator chunkGenerator;

    public CustomGeneratorImpl(String name, ChunkGenerator chunkGenerator) {
        this.name = name;
        this.chunkGenerator = chunkGenerator;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }
}