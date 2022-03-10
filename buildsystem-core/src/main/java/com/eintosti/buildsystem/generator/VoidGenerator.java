/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.generator;

import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

/**
 * @author einTosti
 */
public abstract class VoidGenerator extends ChunkGenerator {

    private final Biome biome;

    public VoidGenerator(Biome biome) {
        this.biome = biome;
    }

    public Biome getBiome() {
        return biome;
    }
}