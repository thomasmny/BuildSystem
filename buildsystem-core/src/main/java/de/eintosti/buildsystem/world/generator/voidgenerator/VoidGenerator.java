/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.generator.voidgenerator;

import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

public abstract class VoidGenerator extends ChunkGenerator {

    private final Biome biome;

    public VoidGenerator(Biome biome) {
        this.biome = biome;
    }

    public Biome getBiome() {
        return biome;
    }
}