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

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Cancels physics-driven events in worlds whose {@link WorldDataKey#PHYSICS} setting is disabled, unless the world
 * re-allows the event's {@link PhysicsCategory}. Explosions are only gated by the master setting; they are owned by
 * {@link WorldDataKey#EXPLOSIONS} and have no category.
 */
@NullMarked
public class BlockPhysicsListener implements Listener {

    private final WorldStorage worldStorage;

    public BlockPhysicsListener(WorldStorage worldStorage) {
        this.worldStorage = worldStorage;
    }

    /**
     * {@return the world's data when physics is disabled there, or {@code null} when physics applies normally}
     */
    private @Nullable WorldData physicsDisabledData(World world) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        if (buildWorld == null) {
            return null;
        }
        WorldData data = buildWorld.getData();
        return data.get(WorldDataKey.PHYSICS) ? null : data;
    }

    private boolean allows(World world, PhysicsCategory category) {
        WorldData data = physicsDisabledData(world);
        return data == null || data.get(category.key());
    }

    private <E extends BlockEvent & Cancellable> void cancelUnlessAllowed(E event, PhysicsCategory category) {
        if (!allows(event.getBlock().getWorld(), category)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        cancelUnlessAllowed(event, PhysicsCategory.LEAF_DECAY);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        cancelUnlessAllowed(event, PhysicsCategory.BLOCK_FADING);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        cancelUnlessAllowed(event, PhysicsCategory.BLOCK_FORMING);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        cancelUnlessAllowed(event, PhysicsCategory.GROWTH);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        cancelUnlessAllowed(event, PhysicsCategory.SPREADING);
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        WorldData data = physicsDisabledData(block.getWorld());
        if (data == null || data.get(PhysicsCategory.BLOCK_UPDATES.key())) {
            return;
        }

        boolean connects = data.get(PhysicsCategory.CONNECTIONS.key()) && canConnect(block);
        event.setCancelled(!connects);
    }

    private static boolean canConnect(Block block) {
        return switch (block.getBlockData()) {
            case Fence _, Gate _, GlassPane _, Stairs _, Wall _ -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        WorldData data = physicsDisabledData(event.getBlock().getWorld());
        if (data == null) {
            return;
        }

        boolean flows = event.getBlock().isLiquid() && data.get(PhysicsCategory.FLUID_FLOW.key());
        event.setCancelled(!flows);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK
                || allows(event.getBlock().getWorld(), PhysicsCategory.FALLING_BLOCKS)) {
            return;
        }
        event.setCancelled(true);
        event.getBlock().getState().update(false, false);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (physicsDisabledData(event.getBlock().getWorld()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        World world = event.getLocation().getWorld();
        if (world != null && physicsDisabledData(world) != null) {
            event.setCancelled(true);
        }
    }
}
