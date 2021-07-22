package de.eintosti.buildsystem.version;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

/**
 * @author einTosti
 */
public class CustomBlock_1_13_R1 implements CustomBlocks {
    private final JavaPlugin plugin;

    public CustomBlock_1_13_R1(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setBlock(BlockPlaceEvent event, String... blockName) {
        Block block = event.getBlockPlaced();
        ItemStack itemStack = event.getItemInHand();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) return;
        if (!itemMeta.hasDisplayName()) return;
        String displayName = itemMeta.getDisplayName();

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            boolean set;

            if (displayName.equals(blockName[0])) {
                block.setType(Material.OAK_WOOD);
                set = true;
            } else if (displayName.equals(blockName[1])) {
                block.setType(Material.SPRUCE_WOOD);
                set = true;
            } else if (displayName.equals(blockName[2])) {
                block.setType(Material.BIRCH_WOOD);
                set = true;
            } else if (displayName.equals(blockName[3])) {
                block.setType(Material.JUNGLE_WOOD);
                set = true;
            } else if (displayName.equals(blockName[4])) {
                block.setType(Material.ACACIA_WOOD);
                set = true;
            } else if (displayName.equals(blockName[5])) {
                block.setType(Material.DARK_OAK_WOOD);
                set = true;
            } else if (displayName.equals(blockName[6])) {
                block.setType(Material.RED_MUSHROOM_BLOCK);
                set = true;
            } else if (displayName.equals(blockName[7])) {
                block.setType(Material.BROWN_MUSHROOM_BLOCK);
                set = true;
            } else if (displayName.equals(blockName[8])) {
                block.setType(Material.MUSHROOM_STEM);
                set = true;
            } else if (displayName.equals(blockName[9])) {
                block.setType(Material.MUSHROOM_STEM);
                MultipleFacing multipleFacing = (MultipleFacing) block.getBlockData();
                multipleFacing.setFace(BlockFace.UP, false);
                multipleFacing.setFace(BlockFace.DOWN, false);
                block.setBlockData(multipleFacing);
                set = true;
            } else if (displayName.equals(blockName[10])) {
                block.setType(Material.MUSHROOM_STEM);
                MultipleFacing multipleFacing = (MultipleFacing) block.getBlockData();
                multipleFacing.setFace(BlockFace.UP, false);
                multipleFacing.setFace(BlockFace.DOWN, false);
                multipleFacing.setFace(BlockFace.NORTH, false);
                multipleFacing.setFace(BlockFace.EAST, false);
                multipleFacing.setFace(BlockFace.SOUTH, false);
                multipleFacing.setFace(BlockFace.WEST, false);
                block.setBlockData(multipleFacing);
                set = true;
            } else if (displayName.equals(blockName[11])) {
                block.setType(Material.SMOOTH_STONE);
                set = true;
            } else if (displayName.equals(blockName[12])) {
                block.setType(Material.STONE_SLAB);
                setDoubleSlab(block);
                set = true;
            } else if (displayName.equals(blockName[13])) {
                block.setType(Material.SMOOTH_SANDSTONE);
                set = true;
            } else if (displayName.equals(blockName[14])) {
                block.setType(Material.SMOOTH_RED_SANDSTONE);
                set = true;
            } else if (displayName.equals(blockName[15])) {
                block.setType(Material.REDSTONE_LAMP);
                powerLamp(block);
                set = true;
            } else if (displayName.equals(blockName[16])) {
                block.setType(Material.FURNACE);
                powerFurnace(player, block);
                set = true;
            } else if (displayName.equals(blockName[17])) {
                block.setType(Material.COMMAND_BLOCK);
                set = true;
            } else if (displayName.equals(blockName[18])) {
                block.setType(Material.BARRIER);
                set = true;
            } else if (displayName.equals(blockName[19])) {
                block.setType(Material.SPAWNER);
                set = true;
            } else if (displayName.equals(blockName[20])) {
                block.setType(Material.NETHER_PORTAL);
                set = true;
            } else if (displayName.equals(blockName[21])) {
                block.setType(Material.END_PORTAL);
                set = true;
            } else if (displayName.equals(blockName[22])) {
                block.setType(Material.DRAGON_EGG);
                set = true;
            } else {
                set = false;
            }
            if (set) event.setCancelled(true);
        });
    }

    @Override
    public void setPlant(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Block adjacent = block.getRelative(event.getBlockFace());
        ItemStack itemStack = event.getItem();

        if (itemStack == null) {
            return;
        }

        adjacent.setType(itemStack.getType());
    }

    @Override
    public void modifySlab(PlayerInteractEvent event) {
        if (event.isCancelled()) return;

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
        open(event.getClickedBlock());
        event.setCancelled(true);
    }

    @Override
    public void toggleIronDoor(PlayerInteractEvent event) {
        open(event.getClickedBlock());
        event.setCancelled(true);
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
        if (block == null) return;
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

    private BlockFace getDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        yaw %= 360;
        int i = (int) ((yaw + 8) / 22.5);
        switch (i) {
            case 15:
            case 0:
            case 1:
            case 2:
                return BlockFace.NORTH;
            case 3:
            case 4:
            case 5:
            case 6:
                return BlockFace.EAST;
            case 7:
            case 8:
            case 9:
            case 10:
                return BlockFace.SOUTH;
            case 11:
            case 12:
            case 13:
            case 14:
                return BlockFace.WEST;
        }
        return BlockFace.NORTH;
    }

    private boolean isTop(Player player, Block block) {
        Location location = player.getEyeLocation().clone();
        while ((!location.getBlock().equals(block)) && location.distance(player.getEyeLocation()) < 6) {
            location.add(player.getLocation().getDirection().multiply(0.06));
        }
        return location.getY() % 1 > 0.5;
    }
}
