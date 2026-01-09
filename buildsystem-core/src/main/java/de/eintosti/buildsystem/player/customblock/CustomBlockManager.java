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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.util.DirectionUtil;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CustomBlockManager implements Listener {

    private final BuildSystemPlugin plugin;

    public CustomBlockManager(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCustomBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(player.getWorld());
        boolean isBuildWorld = buildWorld != null;

        ItemStack itemStack = event.getItemInHand();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (xMaterial != XMaterial.PLAYER_HEAD) {
            return;
        }

        CustomBlock customBlock = CustomBlock.of(itemStack);
        if (customBlock == null) {
            return;
        }

        boolean hadToDisablePhysics = false;
        if (isBuildWorld && !buildWorld.getData().physics().get()) {
            hadToDisablePhysics = true;
            buildWorld.getData().physics().set(true);
        }

        setBlock(event, customBlock);

        if (isBuildWorld && hadToDisablePhysics) {
            buildWorld.getData().physics().set(false);
        }
    }

    /**
     * Sets a custom block based on the provided {@link CustomBlock}.
     *
     * @param event       The {@link BlockPlaceEvent} triggered by the player
     * @param customBlock The custom block to be placed
     */
    public void setBlock(BlockPlaceEvent event, CustomBlock customBlock) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (customBlock) {
                case FULL_OAK_BARCH:
                    block.setType(Material.OAK_WOOD);
                    break;
                case FULL_SPRUCE_BARCH:
                    block.setType(Material.SPRUCE_WOOD);
                    break;
                case FULL_BIRCH_BARCH:
                    block.setType(Material.BIRCH_WOOD);
                    break;
                case FULL_JUNGLE_BARCH:
                    block.setType(Material.JUNGLE_WOOD);
                    break;
                case FULL_ACACIA_BARCH:
                    block.setType(Material.ACACIA_WOOD);
                    break;
                case FULL_DARK_OAK_BARCH:
                    block.setType(Material.DARK_OAK_WOOD);
                    break;
                case RED_MUSHROOM:
                    block.setType(Material.RED_MUSHROOM_BLOCK);
                    break;
                case BROWN_MUSHROOM:
                    block.setType(Material.BROWN_MUSHROOM_BLOCK);
                    break;
                case FULL_MUSHROOM_STEM:
                    block.setType(Material.MUSHROOM_STEM);
                    break;
                case MUSHROOM_STEM:
                    block.setType(Material.MUSHROOM_STEM);
                    MultipleFacing mushroomStem = (MultipleFacing) block.getBlockData();
                    mushroomStem.setFace(BlockFace.UP, false);
                    mushroomStem.setFace(BlockFace.DOWN, false);
                    block.setBlockData(mushroomStem);
                    break;
                case MUSHROOM_BLOCK:
                    block.setType(Material.MUSHROOM_STEM);
                    MultipleFacing mushroomBlock = (MultipleFacing) block.getBlockData();
                    for (BlockFace blockFace : DirectionUtil.BLOCK_SIDES) {
                        mushroomBlock.setFace(blockFace, false);
                    }
                    block.setBlockData(mushroomBlock);
                    break;
                case SMOOTH_STONE:
                    block.setType(Material.SMOOTH_STONE);
                    break;
                case DOUBLE_STONE_SLAB:
                    block.setType(Material.SMOOTH_STONE_SLAB);
                    Slab slab = (Slab) block.getBlockData();
                    slab.setType(Slab.Type.DOUBLE);
                    block.setBlockData(slab);
                    break;
                case SMOOTH_SANDSTONE:
                    block.setType(Material.SMOOTH_SANDSTONE);
                    break;
                case SMOOTH_RED_SANDSTONE:
                    block.setType(Material.SMOOTH_RED_SANDSTONE);
                    break;
                case POWERED_REDSTONE_LAMP:
                    block.setType(Material.REDSTONE_LAMP);
                    Lightable lightable = (Lightable) block.getBlockData();
                    lightable.setLit(true);
                    block.setBlockData(lightable);
                    break;
                case BURNING_FURNACE:
                    block.setType(Material.FURNACE);
                    Furnace furnace = (Furnace) block.getState();
                    furnace.setBurnTime(Short.MAX_VALUE);
                    furnace.update();
                    rotateBlock(block, DirectionUtil.getBlockDirection(player, false));
                    break;
                case PISTON_HEAD:
                    block.setType(Material.PISTON_HEAD);
                    rotateBlock(block, DirectionUtil.getBlockDirection(player, true));
                    break;
                case COMMAND_BLOCK:
                    block.setType(Material.COMMAND_BLOCK);
                    rotateBlock(block, DirectionUtil.getBlockDirection(player, true));
                    break;
                case BARRIER:
                    block.setType(Material.BARRIER);
                    break;
                case INVISIBLE_ITEM_FRAME:
                    // Handled below
                    return;
                case MOB_SPAWNER:
                    block.setType(Material.SPAWNER);
                    break;
                case NETHER_PORTAL:
                    block.setType(Material.NETHER_PORTAL);
                    rotateBlock(block, DirectionUtil.getBlockDirection(player, false));
                    break;
                case END_PORTAL:
                    block.setType(Material.END_PORTAL);
                    break;
                case DRAGON_EGG:
                    block.setType(Material.DRAGON_EGG);
                    break;
                default:
                    return;
            }

            event.setCancelled(true);
        });
    }

    /**
     * Handles the placement of an invisible item frame, making it invisible.
     *
     * @param event The {@link HangingPlaceEvent} triggered when an item frame is placed
     */
    @EventHandler
    public void onInvisibleItemFramePlacement(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame)) {
            return;
        }

        if (CustomBlock.of(event.getItemStack()) != CustomBlock.INVISIBLE_ITEM_FRAME) {
            return;
        }

        itemFrame.setVisible(false);
    }

    /**
     * Rotates a block based on the provided {@link BlockFace} direction.
     *
     * @param block     The block to rotate
     * @param direction The {@link BlockFace} representing the new direction or axis
     */
    public void rotateBlock(Block block, BlockFace direction) {
        switch (block.getBlockData()) {
            case Directional directional -> {
                directional.setFacing(direction);
                block.setBlockData(directional);
            }
            case Orientable orientable -> {
                Axis axis = switch (direction) {
                    case UP, DOWN -> Axis.Y;
                    case EAST, WEST -> Axis.X;
                    default -> Axis.Z;
                };
                orientable.setAxis(axis);
                block.setBlockData(orientable);
            }
            case Sign sign -> {
                sign.setRotation(direction);
                block.setBlockData(sign);
            }
            case HangingSign hangingSign -> {
                hangingSign.setRotation(direction);
                block.setBlockData(hangingSign);
            }
            default -> {
            }
        }
    }
}