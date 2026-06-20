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
package de.eintosti.buildsystem.listener.world;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.util.DirectionUtil;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.MetadataValue;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlockPhysicsListener implements Listener {

    private final WorldStorage worldStorage;
    private final ConfigService configService;

    public BlockPhysicsListener(WorldStorage worldStorage, ConfigService configService) {
        this.worldStorage = worldStorage;
        this.configService = configService;
    }

    private boolean physicsAllowed(World world) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        return buildWorld == null || buildWorld.getData().isPhysics();
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (physicsAllowed(block.getWorld())) {
            return;
        }

        if (!configService.current().world().disabledPhysics().preventConnections()) {
            boolean canConnect =
                    switch (block.getBlockData()) {
                        case Fence fence -> true;
                        case Gate gate -> true;
                        case GlassPane glassPane -> true;
                        case Stairs stairs -> true;
                        case Wall wall -> true;
                        default -> false;
                    };
            if (canConnect) {
                event.setCancelled(false);
                return;
            }
        }

        switch (XMaterial.matchXMaterial(block.getType())) {
            case REDSTONE_BLOCK -> {
                for (BlockFace blockFace : DirectionUtil.BLOCK_SIDES) {
                    if (isCustomRedstoneLamp(block.getRelative(blockFace))) {
                        event.setCancelled(false);
                        return;
                    }
                }
            }
            case REDSTONE_LAMP -> {
                for (BlockFace blockFace : DirectionUtil.BLOCK_SIDES) {
                    if (block.getRelative(blockFace).getType() == XMaterial.REDSTONE_BLOCK.get()) {
                        event.setCancelled(false);
                        return;
                    }
                }
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }

        if (event.getBlock().isLiquid()
                && !configService.current().world().disabledPhysics().preventFluidFlow()) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }

        if (event.getEntityType() == EntityType.FALLING_BLOCK
                && configService.current().world().disabledPhysics().preventFallingBlocks()) {
            event.setCancelled(true);
            event.getBlock().getState().update(false, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (isCustomRedstoneLamp(block)) {
            event.setNewCurrent(15);
        }

        XMaterial xMaterial = XMaterial.matchXMaterial(block.getType());
        if (xMaterial != XMaterial.REDSTONE_BLOCK) {
            return;
        }

        for (BlockFace blockFace : DirectionUtil.BLOCK_SIDES) {
            if (isCustomRedstoneLamp(block.getRelative(blockFace))) {
                event.setNewCurrent(15);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (physicsAllowed(event.getBlock().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        World world = event.getLocation().getWorld();
        if (world == null || physicsAllowed(world)) {
            return;
        }
        event.setCancelled(true);
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
}
