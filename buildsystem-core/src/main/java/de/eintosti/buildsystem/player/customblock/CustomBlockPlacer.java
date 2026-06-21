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
package de.eintosti.buildsystem.player.customblock;

import de.eintosti.buildsystem.util.DirectionUtil;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Maps each {@link CustomBlock} to how it is materialized in the world. Replaces the former giant switch with a per
 * block {@link BlockPlacer} strategy: simple blocks are a bare type change, the handful with extra block data have their
 * own method. {@link CustomBlock#INVISIBLE_ITEM_FRAME} (placed as a hanging entity) and {@link CustomBlock#DEBUG_STICK}
 * have no placer.
 */
@NullMarked
final class CustomBlockPlacer {

    private final Map<CustomBlock, BlockPlacer> placers = new EnumMap<>(CustomBlock.class);

    CustomBlockPlacer() {
        placers.put(CustomBlock.FULL_OAK_BARCH, material(Material.OAK_WOOD));
        placers.put(CustomBlock.FULL_SPRUCE_BARCH, material(Material.SPRUCE_WOOD));
        placers.put(CustomBlock.FULL_BIRCH_BARCH, material(Material.BIRCH_WOOD));
        placers.put(CustomBlock.FULL_JUNGLE_BARCH, material(Material.JUNGLE_WOOD));
        placers.put(CustomBlock.FULL_ACACIA_BARCH, material(Material.ACACIA_WOOD));
        placers.put(CustomBlock.FULL_DARK_OAK_BARCH, material(Material.DARK_OAK_WOOD));
        placers.put(CustomBlock.RED_MUSHROOM, material(Material.RED_MUSHROOM_BLOCK));
        placers.put(CustomBlock.BROWN_MUSHROOM, material(Material.BROWN_MUSHROOM_BLOCK));
        placers.put(CustomBlock.FULL_MUSHROOM_STEM, material(Material.MUSHROOM_STEM));
        placers.put(CustomBlock.MUSHROOM_STEM, CustomBlockPlacer::placeMushroomStem);
        placers.put(CustomBlock.MUSHROOM_BLOCK, CustomBlockPlacer::placeMushroomBlock);
        placers.put(CustomBlock.SMOOTH_STONE, material(Material.SMOOTH_STONE));
        placers.put(CustomBlock.DOUBLE_STONE_SLAB, CustomBlockPlacer::placeDoubleStoneSlab);
        placers.put(CustomBlock.SMOOTH_SANDSTONE, material(Material.SMOOTH_SANDSTONE));
        placers.put(CustomBlock.SMOOTH_RED_SANDSTONE, material(Material.SMOOTH_RED_SANDSTONE));
        placers.put(CustomBlock.POWERED_REDSTONE_LAMP, CustomBlockPlacer::placePoweredRedstoneLamp);
        placers.put(CustomBlock.BURNING_FURNACE, CustomBlockPlacer::placeBurningFurnace);
        placers.put(CustomBlock.PISTON_HEAD, rotated(Material.PISTON_HEAD, true));
        placers.put(CustomBlock.COMMAND_BLOCK, rotated(Material.COMMAND_BLOCK, true));
        placers.put(CustomBlock.BARRIER, material(Material.BARRIER));
        placers.put(CustomBlock.MOB_SPAWNER, material(Material.SPAWNER));
        placers.put(CustomBlock.NETHER_PORTAL, rotated(Material.NETHER_PORTAL, false));
        placers.put(CustomBlock.END_PORTAL, material(Material.END_PORTAL));
        placers.put(CustomBlock.DRAGON_EGG, material(Material.DRAGON_EGG));
    }

    /**
     * Places the custom block at the placed location.
     *
     * @param customBlock The custom block to materialize
     * @param block The block that was placed
     * @param player The placing player, used to orient direction-dependent blocks
     * @return {@code true} if a block was placed (and the original placement should be cancelled), {@code false} for
     *     blocks with no placement (the invisible item frame and the debug stick)
     */
    boolean place(CustomBlock customBlock, Block block, Player player) {
        BlockPlacer placer = placers.get(customBlock);
        if (placer == null) {
            return false;
        }
        placer.place(block, player);
        return true;
    }

    private static BlockPlacer material(Material material) {
        return (block, player) -> block.setType(material);
    }

    private static BlockPlacer rotated(Material material, boolean allowNonCardinal) {
        return (block, player) -> {
            block.setType(material);
            DirectionUtil.rotateBlock(block, DirectionUtil.getBlockDirection(player, allowNonCardinal));
        };
    }

    private static void placeMushroomStem(Block block, Player player) {
        block.setType(Material.MUSHROOM_STEM);
        MultipleFacing mushroomStem = (MultipleFacing) block.getBlockData();
        mushroomStem.setFace(BlockFace.UP, false);
        mushroomStem.setFace(BlockFace.DOWN, false);
        block.setBlockData(mushroomStem);
    }

    private static void placeMushroomBlock(Block block, Player player) {
        block.setType(Material.MUSHROOM_STEM);
        MultipleFacing mushroomBlock = (MultipleFacing) block.getBlockData();
        for (BlockFace blockFace : DirectionUtil.BLOCK_SIDES) {
            mushroomBlock.setFace(blockFace, false);
        }
        block.setBlockData(mushroomBlock);
    }

    private static void placeDoubleStoneSlab(Block block, Player player) {
        block.setType(Material.SMOOTH_STONE_SLAB);
        Slab slab = (Slab) block.getBlockData();
        slab.setType(Slab.Type.DOUBLE);
        block.setBlockData(slab);
    }

    private static void placePoweredRedstoneLamp(Block block, Player player) {
        block.setType(Material.REDSTONE_LAMP);
        Lightable lightable = (Lightable) block.getBlockData();
        lightable.setLit(true);
        block.setBlockData(lightable);
    }

    private static void placeBurningFurnace(Block block, Player player) {
        block.setType(Material.FURNACE);
        Furnace furnace = (Furnace) block.getState();
        furnace.setBurnTime(Short.MAX_VALUE);
        furnace.update();
        DirectionUtil.rotateBlock(block, DirectionUtil.getBlockDirection(player, false));
    }
}
