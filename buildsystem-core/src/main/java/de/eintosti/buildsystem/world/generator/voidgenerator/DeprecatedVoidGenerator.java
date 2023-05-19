/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.generator.voidgenerator;

import org.bukkit.World;
import org.bukkit.block.Biome;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * The {@link VoidGenerator} that is used for servers which run on Spigot 1.13-1.16.
 *
 * @author einTosti
 * @since 2.18.2
 */
@SuppressWarnings("deprecation")
public class DeprecatedVoidGenerator extends VoidGenerator {

    public DeprecatedVoidGenerator() {
        super(Biome.PLAINS);
    }

    @Nonnull
    @Override
    public ChunkData generateChunkData(@Nonnull World world, @Nonnull Random random, int x, int z, @Nonnull BiomeGrid biome) {
        biome.setBiome(x, z, super.getBiome());
        return createChunkData(world);
    }
}