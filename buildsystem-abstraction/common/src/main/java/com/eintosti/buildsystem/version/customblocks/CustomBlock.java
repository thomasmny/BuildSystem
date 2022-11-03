/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.version.customblocks;

import com.eintosti.buildsystem.version.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

/**
 * @author einTosti
 */
public enum CustomBlock {
    BARRIER("blocks_barrier"),
    BROWN_MUSHROOM("blocks_brown_mushroom"),
    BURNING_FURNACE("blocks_burning_furnace"),
    COMMAND_BLOCK("blocks_command_block"),
    DOUBLE_STONE_SLAB("blocks_double_stone_slab"),
    DRAGON_EGG("blocks_dragon_egg"),
    END_PORTAL("blocks_end_portal"),
    FULL_ACACIA_BARCH("blocks_full_acacia_barch"),
    FULL_BIRCH_BARCH("blocks_full_birch_barch"),
    FULL_DARK_OAK_BARCH("blocks_full_dark_oak_barch"),
    FULL_JUNGLE_BARCH("blocks_full_jungle_barch"),
    FULL_MUSHROOM_STEM("blocks_full_mushroom_stem"),
    FULL_OAK_BARCH("blocks_full_oak_barch"),
    FULL_SPRUCE_BARCH("blocks_full_spruce_barch"),
    INVISIBLE_ITEM_FRAME("blocks_invisible_item_frame", MinecraftVersion.CAVES_17),
    MOB_SPAWNER("blocks_mob_spawner"),
    MUSHROOM_BLOCK("blocks_mushroom_block"),
    MUSHROOM_STEM("blocks_mushroom_stem"),
    NETHER_PORTAL("blocks_nether_portal"),
    PISTON_HEAD("blocks_piston_head"),
    REDSTONE_LAMP("blocks_powered_redstone_lamp"),
    RED_MUSHROOM("blocks_red_mushroom"),
    SMOOTH_RED_SANDSTONE("blocks_smooth_red_sandstone"),
    SMOOTH_SANDSTONE("blocks_smooth_sandstone"),
    SMOOTH_STONE("blocks_smooth_stone");

    private final String key;
    private final MinecraftVersion version;

    CustomBlock(String key) {
        this.key = key;
        this.version = MinecraftVersion.BOUNTIFUL_8;
    }

    CustomBlock(String key, MinecraftVersion version) {
        this.key = key;
        this.version = version;
    }

    @Nullable
    public static CustomBlock getCustomBlock(String key) {
        for (CustomBlock customBlock : values()) {
            if (customBlock.getKey().equals(key)) {
                return customBlock;
            }
        }
        return null;
    }

    public String getKey() {
        return key;
    }

    public MinecraftVersion getVersion() {
        return version;
    }
}