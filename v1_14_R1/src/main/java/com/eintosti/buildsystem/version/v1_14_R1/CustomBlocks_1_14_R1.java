/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.version.v1_14_R1;

import com.eintosti.buildsystem.version.CustomBlocks;
import com.eintosti.buildsystem.version.DirectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * @author einTosti
 */
public class CustomBlocks_1_14_R1 extends DirectionUtils implements CustomBlocks {
    private final JavaPlugin plugin;

    public CustomBlocks_1_14_R1(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setBlock(BlockPlaceEvent event, String... blockName) {
        Block block = event.getBlockPlaced();
        ItemStack itemStack = event.getItemInHand();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null || !itemMeta.hasDisplayName()) return;
        String displayName = itemMeta.getDisplayName();
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (Arrays.asList(blockName).indexOf(displayName)) {
                case 0:
                    block.setType(Material.OAK_WOOD);
                    break;
                case 1:
                    block.setType(Material.SPRUCE_WOOD);
                    break;
                case 2:
                    block.setType(Material.BIRCH_WOOD);
                    break;
                case 3:
                    block.setType(Material.JUNGLE_WOOD);
                    break;
                case 4:
                    block.setType(Material.ACACIA_WOOD);
                    break;
                case 5:
                    block.setType(Material.DARK_OAK_WOOD);
                    break;
                case 6:
                    block.setType(Material.RED_MUSHROOM_BLOCK);
                    break;
                case 7:
                    block.setType(Material.BROWN_MUSHROOM_BLOCK);
                    break;
                case 8:
                    block.setType(Material.MUSHROOM_STEM);
                    break;
                case 9:
                    block.setType(Material.MUSHROOM_STEM);
                    MultipleFacing block9Data = (MultipleFacing) block.getBlockData();
                    block9Data.setFace(BlockFace.UP, false);
                    block9Data.setFace(BlockFace.DOWN, false);
                    block.setBlockData(block9Data);
                    break;
                case 10:
                    block.setType(Material.MUSHROOM_STEM);
                    MultipleFacing block10Data = (MultipleFacing) block.getBlockData();
                    block10Data.setFace(BlockFace.UP, false);
                    block10Data.setFace(BlockFace.DOWN, false);
                    block10Data.setFace(BlockFace.NORTH, false);
                    block10Data.setFace(BlockFace.EAST, false);
                    block10Data.setFace(BlockFace.SOUTH, false);
                    block10Data.setFace(BlockFace.WEST, false);
                    block.setBlockData(block10Data);
                    break;
                case 11:
                    block.setType(Material.SMOOTH_STONE);
                    break;
                case 12:
                    block.setType(Material.STONE_SLAB);
                    setDoubleSlab(block);
                    break;
                case 13:
                    block.setType(Material.SMOOTH_SANDSTONE);
                    break;
                case 14:
                    block.setType(Material.SMOOTH_RED_SANDSTONE);
                    break;
                case 15:
                    block.setType(Material.REDSTONE_LAMP);
                    powerLamp(block);
                    break;
                case 16:
                    block.setType(Material.FURNACE);
                    powerFurnace(player, block);
                    break;
                case 17:
                    block.setType(Material.COMMAND_BLOCK);
                    break;
                case 18:
                    block.setType(Material.BARRIER);
                    break;
                case 19:
                    block.setType(Material.SPAWNER);
                    break;
                case 20:
                    block.setType(Material.NETHER_PORTAL);
                    break;
                case 21:
                    block.setType(Material.END_PORTAL);
                    break;
                case 22:
                    block.setType(Material.DRAGON_EGG);
                    break;
                default:
                    return;
            }

            event.setCancelled(true);
        });
    }

    @Override
    public void setPlant(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Block adjacent = block.getRelative(event.getBlockFace());
        adjacent.setType(event.getItem().getType());
    }

    @Override
    public void modifySlab(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!(block.getBlockData() instanceof Slab)) return;
        Slab slab = (Slab) block.getBlockData();
        if (slab.getType() != Slab.Type.DOUBLE) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (isTop(player, block)) {
            slab.setType(Slab.Type.BOTTOM);
        } else {
            slab.setType(Slab.Type.TOP);
        }

        block.setBlockData(slab);
    }

    private void setDoubleSlab(Block block) {
        Slab slab = (Slab) block.getBlockData();
        slab.setType(Slab.Type.DOUBLE);
        block.setBlockData(slab);
    }

    @Override
    public void toggleIronTrapdoor(PlayerInteractEvent event) {
        event.setCancelled(true);
        open(event.getClickedBlock());
    }

    @Override
    public void toggleIronDoor(PlayerInteractEvent event) {
        event.setCancelled(true);
        open(event.getClickedBlock());
    }

    @Override
    public void rotate(Block block, Player player, BlockFace blockFace) {
        BlockFace direction = blockFace != null ? blockFace : getDirection(player);
        BlockData blockData = block.getBlockData();

        if (blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            directional.setFacing(direction);
            block.setBlockData(directional);
        } else if (blockData instanceof Sign) {
            Sign sign = (Sign) blockData;
            sign.setRotation(direction);
            block.setBlockData(sign);
        }
    }

    private void open(Block block) {
        if (block == null) {
            return;
        }

        Openable openable = (Openable) block.getBlockData();
        openable.setOpen(!openable.isOpen());
        block.setBlockData(openable);
    }

    private void powerLamp(Block block) {
        Lightable lightable = (Lightable) block.getBlockData();
        lightable.setLit(true);
        block.setBlockData(lightable);
    }

    private void powerFurnace(Player player, Block block) {
        rotate(block, player, null);
        Furnace furnace = (Furnace) block.getState();
        furnace.setBurnTime(Short.MAX_VALUE);
        furnace.update();
    }
}

