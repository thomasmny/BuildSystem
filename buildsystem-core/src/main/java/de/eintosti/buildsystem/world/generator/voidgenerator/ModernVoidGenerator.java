/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.generator.voidgenerator;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The {@link VoidGenerator} that is used for servers which run on Spigot 1.17+
 *
 * @author einTosti
 * @since 2.18.2
 */
public class ModernVoidGenerator extends VoidGenerator {

    public ModernVoidGenerator() {
        super(Biome.PLAINS);
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkData chunkData) {
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new SingleBiomeProvider(super.getBiome());
    }

    public static class SingleBiomeProvider extends BiomeProvider {

        private final Biome biome;

        public SingleBiomeProvider(Biome biome) {
            this.biome = biome;
        }

        @NotNull
        @Override
        public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            return this.biome;
        }

        @NotNull
        @Override
        public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return Collections.singletonList(this.biome);
        }
    }
}