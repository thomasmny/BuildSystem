/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.object.world.generator;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * @author einTosti
 */
public class VoidGenerator extends ChunkGenerator {
    private final int minY;

    public VoidGenerator() {
        this.minY = XMaterial.supports(18) ? -64 : 0;
    }

    @Override
    @NotNull
    @SuppressWarnings("deprecation")
    public ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
        for (int blockX = 0; blockX < 16; blockX++) {
            for (int blockY = minY; blockY < world.getMaxHeight(); blockY++) {
                for (int blockZ = 0; blockZ < 16; blockZ++) {
                    biome.setBiome(blockX, blockY, blockZ, Biome.PLAINS);
                }
            }
        }

        return createChunkData(world);
    }
}
