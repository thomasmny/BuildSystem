package de.eintosti.buildsystem.version;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author einTosti
 */
public class CustomBlock_1_12_R1 implements CustomBlocks {
    private final JavaPlugin plugin;

    private final int mcVersion;

    public CustomBlock_1_12_R1(JavaPlugin plugin) {
        this.plugin = plugin;

        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        this.mcVersion = Integer.parseInt(version.replaceAll("[^0-9]", ""));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlock(BlockPlaceEvent event, String... blockName) {
        Block block = event.getBlockPlaced();
        ItemStack itemStack = event.getItemInHand();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) return;
        if (!itemMeta.hasDisplayName()) return;
        String displayName = itemMeta.getDisplayName();

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean set;

            if (displayName.equals(blockName[0])) {
                block.setType(Material.LOG);
                block.setData((byte) 12, true);
                set = true;
            } else if (displayName.equals(blockName[1])) {
                block.setType(Material.LOG);
                block.setData((byte) 13, true);
                set = true;
            } else if (displayName.equals(blockName[2])) {
                block.setType(Material.LOG);
                block.setData((byte) 14, true);
                set = true;
            } else if (displayName.equals(blockName[3])) {
                block.setType(Material.LOG);
                block.setData((byte) 15, true);
                set = true;
            } else if (displayName.equals(blockName[4])) {
                block.setType(Material.LOG_2);
                block.setData((byte) 12, true);
                set = true;
            } else if (displayName.equals(blockName[5])) {
                block.setType(Material.LOG_2);
                block.setData((byte) 13, true);
                set = true;
            } else if (displayName.equals(blockName[6])) {
                block.setType(Material.HUGE_MUSHROOM_2);
                set = true;
            } else if (displayName.equals(blockName[7])) {
                block.setType(Material.HUGE_MUSHROOM_1);
                set = true;
            } else if (displayName.equals(blockName[8])) {
                block.setType(Material.HUGE_MUSHROOM_1);
                block.setData((byte) 15, true);
                set = true;
            } else if (displayName.equals(blockName[9])) {
                block.setType(Material.HUGE_MUSHROOM_1);
                block.setData((byte) 10, true);
                set = true;
            } else if (displayName.equals(blockName[10])) {
                block.setType(Material.HUGE_MUSHROOM_1);
                block.setData((byte) 0, true);
                set = true;
            } else if (displayName.equals(blockName[11])) {
                block.setType(Material.DOUBLE_STEP);
                block.setData((byte) 8, true);
                set = true;
            } else if (displayName.equals(blockName[12])) {
                block.setType(Material.DOUBLE_STEP);
                block.setData((byte) 0, true);
                set = true;
            } else if (displayName.equals(blockName[13])) {
                block.setType(Material.DOUBLE_STEP);
                block.setData((byte) 9, true);
                set = true;
            } else if (displayName.equals(blockName[14])) {
                block.setTypeId(181, true);
                block.setData((byte) 8, true);
                set = true;
            } else if (displayName.equals(blockName[15])) {
                block.setType(Material.REDSTONE_LAMP_ON);
                powerLamp(block);
                set = true;
            } else if (displayName.equals(blockName[16])) {
                block.setType(Material.BURNING_FURNACE);
                powerFurnace(block);
                rotate(block, event.getPlayer(), null);
                set = true;
            } else if (displayName.equals(blockName[17])) {
                block.setType(Material.COMMAND);
                set = true;
            } else if (displayName.equals(blockName[18])) {
                block.setType(Material.BARRIER);
                set = true;
            } else if (displayName.equals(blockName[19])) {
                block.setType(Material.MOB_SPAWNER);
                set = true;
            } else if (displayName.equals(blockName[20])) {
                block.setType(Material.PORTAL);
                set = true;
            } else if (displayName.equals(blockName[21])) {
                block.setType(Material.ENDER_PORTAL);
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
    @SuppressWarnings("deprecation")
    public void setPlant(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Block adjacent = block.getRelative(event.getBlockFace());
        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;

        adjacent.setType(itemStack.getType(), false);
        adjacent.setData((byte) itemStack.getDurability());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void modifySlab(PlayerInteractEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Material material = block.getType();

        if (mcVersion >= 190) {
            switch (material) {
                case STEP:
                case WOOD_STEP:
                case STONE_SLAB2:
                case PURPUR_SLAB:
                    return;
            }
        } else {
            switch (material) {
                case STEP:
                case WOOD_STEP:
                case STONE_SLAB2:
                    return;
            }
        }

        Material changedMaterial = null;
        switch (material) {
            case DOUBLE_STEP:
                changedMaterial = Material.STEP;
                break;
            case WOOD_DOUBLE_STEP:
                changedMaterial = Material.WOOD_STEP;
                break;
            case DOUBLE_STONE_SLAB2:
                changedMaterial = Material.STONE_SLAB2;
                break;
        }

        if (mcVersion >= 190) {
            if (material.equals(Material.PURPUR_DOUBLE_SLAB)) {
                changedMaterial = Material.PURPUR_SLAB;
            }
        }

        if (changedMaterial == null) return;

        if (block.getData() <= 7) {
            Player player = event.getPlayer();
            event.setCancelled(true);

            byte data = block.getData();
            if (isTop(player, block)) {
                block.setType(changedMaterial);
                block.setData(data);
            } else {
                block.setType(changedMaterial);
                block.setData((byte) (data + 8));
            }
        }
    }

    @Override
    public void toggleIronTrapdoor(PlayerInteractEvent event) {
        BlockState blockState = event.getClickedBlock().getState();
        TrapDoor trapDoor = (TrapDoor) blockState.getData();
        trapDoor.setOpen(!trapDoor.isOpen());
        blockState.update();
    }

    @Override
    public void toggleIronDoor(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        BlockState blockState = clickedBlock.getState();
        Door door = (Door) blockState.getData();

        if (door.isTopHalf()) {
            Block bottomBlock = clickedBlock.getRelative(BlockFace.DOWN);
            BlockState bottomBlockState = bottomBlock.getState();
            Door bottomDoor = (Door) bottomBlockState.getData();
            bottomDoor.setOpen(!bottomDoor.isOpen());
            bottomBlockState.update();
        } else {
            door.setOpen(!door.isOpen());
            blockState.update();
        }
    }

    @Override
    public void rotate(Block block, Player player, BlockFace blockFace) {
        BlockState state = block.getState();
        MaterialData data = state.getData();
        if (data instanceof Directional) {
            ((Directional) data).setFacingDirection(blockFace != null ? blockFace : getDirection(player));
            state.update(true);
        }
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

    private void powerLamp(Block block) {
        Block redstoneBlock = block.getLocation().add(0, 1, 0).getBlock();
        Material originalMaterial = redstoneBlock.getType();
        BlockState originalState = redstoneBlock.getState();
        MaterialData originalMaterialData = originalState.getData();

        redstoneBlock.setType(Material.REDSTONE_BLOCK, true);
        redstoneBlock.setType(originalMaterial, false);
        originalState.setData(originalMaterialData);
        originalState.update(true, false);

        block.setMetadata("CustomRedstoneLamp", new FixedMetadataValue(plugin, true));
    }

    private void powerFurnace(Block block) {
        Furnace furnace = (Furnace) block.getState();
        furnace.setBurnTime(Short.MAX_VALUE);
        furnace.update();
    }

    private boolean isTop(Player player, Block block) {
        Location location = player.getEyeLocation().clone();
        while ((!location.getBlock().equals(block)) && location.distance(player.getEyeLocation()) < 6) {
            location.add(player.getLocation().getDirection().multiply(0.06));
        }
        return location.getY() % 1 > 0.5;
    }
}
