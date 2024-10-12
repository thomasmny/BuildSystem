/*
 * Copyright (c) 2018-2023, Thomas Meaney
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
package de.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XMaterial;

/**
 * @author Trichtern
 */
public final class MaterialUtils {

    private MaterialUtils() {
    }

    /**
     * Checks if this Material can be interacted with.
     * <p>
     * Interactable materials include those with functionality when they are interacted with by a player such as chests,
     * furnaces, etc. Some blocks such as piston heads and stairs are considered interactable though may not perform any
     * additional functionality. Note that the interactability of some materials may be dependent on their state as
     * well. This method will return true if there is at least one state in which additional interact handling is
     * performed for the material.
     *
     * @param xMaterial The material to check
     * @return {@code true} if this material can be interacted with.
     */
    public static boolean isInteractable(XMaterial material) {
        switch (material) {
            case ACACIA_BUTTON:
            case ACACIA_DOOR:
            case ACACIA_FENCE:
            case ACACIA_FENCE_GATE:
            case ACACIA_HANGING_SIGN:
            case ACACIA_SIGN:
            case ACACIA_STAIRS:
            case ACACIA_TRAPDOOR:
            case ACACIA_WALL_HANGING_SIGN:
            case ACACIA_WALL_SIGN:
            case ANDESITE_STAIRS:
            case ANVIL:
            case BAMBOO_BUTTON:
            case BAMBOO_DOOR:
            case BAMBOO_FENCE:
            case BAMBOO_FENCE_GATE:
            case BAMBOO_HANGING_SIGN:
            case BAMBOO_MOSAIC_STAIRS:
            case BAMBOO_SIGN:
            case BAMBOO_STAIRS:
            case BAMBOO_TRAPDOOR:
            case BAMBOO_WALL_HANGING_SIGN:
            case BAMBOO_WALL_SIGN:
            case BARREL:
            case BEACON:
            case BEEHIVE:
            case BEE_NEST:
            case BELL:
            case BIRCH_BUTTON:
            case BIRCH_DOOR:
            case BIRCH_FENCE:
            case BIRCH_FENCE_GATE:
            case BIRCH_HANGING_SIGN:
            case BIRCH_SIGN:
            case BIRCH_STAIRS:
            case BIRCH_TRAPDOOR:
            case BIRCH_WALL_HANGING_SIGN:
            case BIRCH_WALL_SIGN:
            case BLACKSTONE_STAIRS:
            case BLACK_BED:
            case BLACK_CANDLE:
            case BLACK_CANDLE_CAKE:
            case BLACK_SHULKER_BOX:
            case BLAST_FURNACE:
            case BLUE_BED:
            case BLUE_CANDLE:
            case BLUE_CANDLE_CAKE:
            case BLUE_SHULKER_BOX:
            case BREWING_STAND:
            case BRICK_STAIRS:
            case BROWN_BED:
            case BROWN_CANDLE:
            case BROWN_CANDLE_CAKE:
            case BROWN_SHULKER_BOX:
            case CAKE:
            case CAMPFIRE:
            case CANDLE:
            case CANDLE_CAKE:
            case CARTOGRAPHY_TABLE:
            case CAULDRON:
            case CAVE_VINES:
            case CAVE_VINES_PLANT:
            case CHAIN_COMMAND_BLOCK:
            case CHERRY_BUTTON:
            case CHERRY_DOOR:
            case CHERRY_FENCE:
            case CHERRY_FENCE_GATE:
            case CHERRY_HANGING_SIGN:
            case CHERRY_SIGN:
            case CHERRY_STAIRS:
            case CHERRY_TRAPDOOR:
            case CHERRY_WALL_HANGING_SIGN:
            case CHERRY_WALL_SIGN:
            case CHEST:
            case CHIPPED_ANVIL:
            case CHISELED_BOOKSHELF:
            case COBBLED_DEEPSLATE_STAIRS:
            case COBBLESTONE_STAIRS:
            case COMMAND_BLOCK:
            case COMPARATOR:
            case COMPOSTER:
            case CRAFTING_TABLE:
            case CRIMSON_BUTTON:
            case CRIMSON_DOOR:
            case CRIMSON_FENCE:
            case CRIMSON_FENCE_GATE:
            case CRIMSON_HANGING_SIGN:
            case CRIMSON_SIGN:
            case CRIMSON_STAIRS:
            case CRIMSON_TRAPDOOR:
            case CRIMSON_WALL_HANGING_SIGN:
            case CRIMSON_WALL_SIGN:
            case CUT_COPPER_STAIRS:
            case CYAN_BED:
            case CYAN_CANDLE:
            case CYAN_CANDLE_CAKE:
            case CYAN_SHULKER_BOX:
            case DAMAGED_ANVIL:
            case DARK_OAK_BUTTON:
            case DARK_OAK_DOOR:
            case DARK_OAK_FENCE:
            case DARK_OAK_FENCE_GATE:
            case DARK_OAK_HANGING_SIGN:
            case DARK_OAK_SIGN:
            case DARK_OAK_STAIRS:
            case DARK_OAK_TRAPDOOR:
            case DARK_OAK_WALL_HANGING_SIGN:
            case DARK_OAK_WALL_SIGN:
            case DARK_PRISMARINE_STAIRS:
            case DAYLIGHT_DETECTOR:
            case DEEPSLATE_BRICK_STAIRS:
            case DEEPSLATE_REDSTONE_ORE:
            case DEEPSLATE_TILE_STAIRS:
            case DIORITE_STAIRS:
            case DISPENSER:
            case DRAGON_EGG:
            case DROPPER:
            case ENCHANTING_TABLE:
            case ENDER_CHEST:
            case END_STONE_BRICK_STAIRS:
            case EXPOSED_CUT_COPPER_STAIRS:
            case FLETCHING_TABLE:
            case FLOWER_POT:
            case FURNACE:
            case GRANITE_STAIRS:
            case GRAY_BED:
            case GRAY_CANDLE:
            case GRAY_CANDLE_CAKE:
            case GRAY_SHULKER_BOX:
            case GREEN_BED:
            case GREEN_CANDLE:
            case GREEN_CANDLE_CAKE:
            case GREEN_SHULKER_BOX:
            case GRINDSTONE:
            case HOPPER:
            case IRON_DOOR:
            case IRON_TRAPDOOR:
            case JIGSAW:
            case JUKEBOX:
            case JUNGLE_BUTTON:
            case JUNGLE_DOOR:
            case JUNGLE_FENCE:
            case JUNGLE_FENCE_GATE:
            case JUNGLE_HANGING_SIGN:
            case JUNGLE_SIGN:
            case JUNGLE_STAIRS:
            case JUNGLE_TRAPDOOR:
            case JUNGLE_WALL_HANGING_SIGN:
            case JUNGLE_WALL_SIGN:
            case LAVA_CAULDRON:
            case LECTERN:
            case LEVER:
            case LIGHT:
            case LIGHT_BLUE_BED:
            case LIGHT_BLUE_CANDLE:
            case LIGHT_BLUE_CANDLE_CAKE:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_GRAY_BED:
            case LIGHT_GRAY_CANDLE:
            case LIGHT_GRAY_CANDLE_CAKE:
            case LIGHT_GRAY_SHULKER_BOX:
            case LIME_BED:
            case LIME_CANDLE:
            case LIME_CANDLE_CAKE:
            case LIME_SHULKER_BOX:
            case LOOM:
            case MAGENTA_BED:
            case MAGENTA_CANDLE:
            case MAGENTA_CANDLE_CAKE:
            case MAGENTA_SHULKER_BOX:
            case MANGROVE_BUTTON:
            case MANGROVE_DOOR:
            case MANGROVE_FENCE:
            case MANGROVE_FENCE_GATE:
            case MANGROVE_HANGING_SIGN:
            case MANGROVE_SIGN:
            case MANGROVE_STAIRS:
            case MANGROVE_TRAPDOOR:
            case MANGROVE_WALL_HANGING_SIGN:
            case MANGROVE_WALL_SIGN:
            case MOSSY_COBBLESTONE_STAIRS:
            case MOSSY_STONE_BRICK_STAIRS:
            case MOVING_PISTON:
            case MUD_BRICK_STAIRS:
            case NETHER_BRICK_FENCE:
            case NETHER_BRICK_STAIRS:
            case NOTE_BLOCK:
            case OAK_BUTTON:
            case OAK_DOOR:
            case OAK_FENCE:
            case OAK_FENCE_GATE:
            case OAK_HANGING_SIGN:
            case OAK_SIGN:
            case OAK_STAIRS:
            case OAK_TRAPDOOR:
            case OAK_WALL_HANGING_SIGN:
            case OAK_WALL_SIGN:
            case ORANGE_BED:
            case ORANGE_CANDLE:
            case ORANGE_CANDLE_CAKE:
            case ORANGE_SHULKER_BOX:
            case OXIDIZED_CUT_COPPER_STAIRS:
            case PINK_BED:
            case PINK_CANDLE:
            case PINK_CANDLE_CAKE:
            case PINK_SHULKER_BOX:
            case POLISHED_ANDESITE_STAIRS:
            case POLISHED_BLACKSTONE_BRICK_STAIRS:
            case POLISHED_BLACKSTONE_BUTTON:
            case POLISHED_BLACKSTONE_STAIRS:
            case POLISHED_DEEPSLATE_STAIRS:
            case POLISHED_DIORITE_STAIRS:
            case POLISHED_GRANITE_STAIRS:
            case POTTED_ACACIA_SAPLING:
            case POTTED_ALLIUM:
            case POTTED_AZALEA_BUSH:
            case POTTED_AZURE_BLUET:
            case POTTED_BAMBOO:
            case POTTED_BIRCH_SAPLING:
            case POTTED_BLUE_ORCHID:
            case POTTED_BROWN_MUSHROOM:
            case POTTED_CACTUS:
            case POTTED_CHERRY_SAPLING:
            case POTTED_CORNFLOWER:
            case POTTED_CRIMSON_FUNGUS:
            case POTTED_CRIMSON_ROOTS:
            case POTTED_DANDELION:
            case POTTED_DARK_OAK_SAPLING:
            case POTTED_DEAD_BUSH:
            case POTTED_FERN:
            case POTTED_FLOWERING_AZALEA_BUSH:
            case POTTED_JUNGLE_SAPLING:
            case POTTED_LILY_OF_THE_VALLEY:
            case POTTED_MANGROVE_PROPAGULE:
            case POTTED_OAK_SAPLING:
            case POTTED_ORANGE_TULIP:
            case POTTED_OXEYE_DAISY:
            case POTTED_PINK_TULIP:
            case POTTED_POPPY:
            case POTTED_RED_MUSHROOM:
            case POTTED_RED_TULIP:
            case POTTED_SPRUCE_SAPLING:
            case POTTED_TORCHFLOWER:
            case POTTED_WARPED_FUNGUS:
            case POTTED_WARPED_ROOTS:
            case POTTED_WHITE_TULIP:
            case POTTED_WITHER_ROSE:
            case POWDER_SNOW_CAULDRON:
            case PRISMARINE_BRICK_STAIRS:
            case PRISMARINE_STAIRS:
            case PUMPKIN:
            case PURPLE_BED:
            case PURPLE_CANDLE:
            case PURPLE_CANDLE_CAKE:
            case PURPLE_SHULKER_BOX:
            case PURPUR_STAIRS:
            case QUARTZ_STAIRS:
            case REDSTONE_ORE:
            case REDSTONE_WIRE:
            case RED_BED:
            case RED_CANDLE:
            case RED_CANDLE_CAKE:
            case RED_NETHER_BRICK_STAIRS:
            case RED_SANDSTONE_STAIRS:
            case RED_SHULKER_BOX:
            case REPEATER:
            case REPEATING_COMMAND_BLOCK:
            case RESPAWN_ANCHOR:
            case SANDSTONE_STAIRS:
            case SHULKER_BOX:
            case SMITHING_TABLE:
            case SMOKER:
            case SMOOTH_QUARTZ_STAIRS:
            case SMOOTH_RED_SANDSTONE_STAIRS:
            case SMOOTH_SANDSTONE_STAIRS:
            case SOUL_CAMPFIRE:
            case SPRUCE_BUTTON:
            case SPRUCE_DOOR:
            case SPRUCE_FENCE:
            case SPRUCE_FENCE_GATE:
            case SPRUCE_HANGING_SIGN:
            case SPRUCE_SIGN:
            case SPRUCE_STAIRS:
            case SPRUCE_TRAPDOOR:
            case SPRUCE_WALL_HANGING_SIGN:
            case SPRUCE_WALL_SIGN:
            case STONECUTTER:
            case STONE_BRICK_STAIRS:
            case STONE_BUTTON:
            case STONE_STAIRS:
            case STRUCTURE_BLOCK:
            case SWEET_BERRY_BUSH:
            case TNT:
            case TRAPPED_CHEST:
            case WARPED_BUTTON:
            case WARPED_DOOR:
            case WARPED_FENCE:
            case WARPED_FENCE_GATE:
            case WARPED_HANGING_SIGN:
            case WARPED_SIGN:
            case WARPED_STAIRS:
            case WARPED_TRAPDOOR:
            case WARPED_WALL_HANGING_SIGN:
            case WARPED_WALL_SIGN:
            case WATER_CAULDRON:
            case WAXED_CUT_COPPER_STAIRS:
            case WAXED_EXPOSED_CUT_COPPER_STAIRS:
            case WAXED_OXIDIZED_CUT_COPPER_STAIRS:
            case WAXED_WEATHERED_CUT_COPPER_STAIRS:
            case WEATHERED_CUT_COPPER_STAIRS:
            case WHITE_BED:
            case WHITE_CANDLE:
            case WHITE_CANDLE_CAKE:
            case WHITE_SHULKER_BOX:
            case YELLOW_BED:
            case YELLOW_CANDLE:
            case YELLOW_CANDLE_CAKE:
            case YELLOW_SHULKER_BOX:
                return true;
            default:
                return false;
        }
    }
}