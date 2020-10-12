package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

/**
 * @author einTosti
 */
public class BlockPhysicsListener implements Listener {
    private final WorldManager worldManager;

    public BlockPhysicsListener(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        org.bukkit.World bukkitWorld = block.getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());

        if (world != null) {
            if (!world.isPhysics()) {
                XMaterial xMaterial = XMaterial.matchXMaterial(block.getType());
                BlockFace[] surroundingBlocks = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

                if (xMaterial == XMaterial.REDSTONE_BLOCK) {
                    for (BlockFace blockFace : surroundingBlocks) {
                        if (isCustomRedstoneLamp(block.getRelative(blockFace))) {
                            event.setCancelled(false);
                            return;
                        }
                    }
                }
                if (xMaterial == XMaterial.REDSTONE_LAMP) {
                    for (BlockFace blockFace : surroundingBlocks) {
                        if (block.getRelative(blockFace).getType() == XMaterial.REDSTONE_BLOCK.parseMaterial()) {
                            event.setCancelled(false);
                            return;
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    private boolean isCustomRedstoneLamp(Block block) {
        List<MetadataValue> metadataValues = block.getMetadata("CustomRedstoneLamp");
        for (MetadataValue value : metadataValues) {
            if (value.asBoolean()) {
                return true;
            }
        }
        return block.getType().name().equals("REDSTONE_LAMP_ON");
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isPhysics()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isPhysics()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isPhysics()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isPhysics()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isPhysics()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isPhysics()) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());

        if (world == null) return;
        if (world.isPhysics()) return;
        if (event.getEntityType().equals(EntityType.FALLING_BLOCK)) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        XMaterial xMaterial = XMaterial.matchXMaterial(block.getType());

        if (isCustomRedstoneLamp(block)) {
            event.setNewCurrent(15);
        }

        if (xMaterial == XMaterial.REDSTONE_BLOCK) {
            for (BlockFace blockFace : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isCustomRedstoneLamp(block.getRelative(blockFace))) {
                    event.setNewCurrent(15);
                }
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        org.bukkit.World bukkitWorld = event.getBlock().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isExplosions()) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        org.bukkit.World bukkitWorld = event.getLocation().getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());
        if (world == null) return;
        if (!world.isExplosions()) event.setCancelled(true);
    }
}
