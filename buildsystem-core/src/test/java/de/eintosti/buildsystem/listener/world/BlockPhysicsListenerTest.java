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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises the master-and-exceptions gate of {@link BlockPhysicsListener}: physics enabled leaves events alone,
 * physics disabled cancels them, and a re-allowed {@link PhysicsCategory} exempts exactly its event family.
 */
@NullMarked
class BlockPhysicsListenerTest {

    private final World world = mock(World.class);

    private WorldDataImpl data;
    private BlockPhysicsListener listener;

    @BeforeEach
    void setUp() {
        data = new WorldDataImpl.WorldDataBuilder("world")
                .withStatus(TestData.NOT_STARTED)
                .build();
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getData()).thenReturn(data);
        WorldStorage worldStorage = mock(WorldStorage.class);
        when(worldStorage.getBuildWorld(world)).thenReturn(buildWorld);
        listener = new BlockPhysicsListener(worldStorage);
    }

    private Block block() {
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        return block;
    }

    /**
     * Runs the three gate states against fresh events: physics on (not cancelled), physics off (cancelled), physics
     * off with the category re-allowed (not cancelled).
     */
    private <E extends Cancellable> void assertCategoryGate(
            Supplier<E> eventFactory, Consumer<E> handler, PhysicsCategory category) {
        E event = eventFactory.get();
        handler.accept(event);
        assertFalse(event.isCancelled(), category + ": enabled physics must not cancel");

        data.set(WorldDataKey.PHYSICS, false);
        event = eventFactory.get();
        handler.accept(event);
        assertTrue(event.isCancelled(), category + ": disabled physics must cancel");

        data.set(category.key(), true);
        event = eventFactory.get();
        handler.accept(event);
        assertFalse(event.isCancelled(), category + ": re-allowed category must not cancel");
    }

    @Test
    void leavesDecay_gatedByLeafDecayCategory() {
        assertCategoryGate(() -> new LeavesDecayEvent(block()), listener::onLeavesDecay, PhysicsCategory.LEAF_DECAY);
    }

    @Test
    void blockFade_gatedByBlockFadingCategory() {
        assertCategoryGate(
                () -> new BlockFadeEvent(block(), mock(BlockState.class)),
                listener::onBlockFade,
                PhysicsCategory.BLOCK_FADING);
    }

    @Test
    void blockForm_gatedByBlockFormingCategory() {
        assertCategoryGate(
                () -> new BlockFormEvent(block(), mock(BlockState.class)),
                listener::onBlockForm,
                PhysicsCategory.BLOCK_FORMING);
    }

    @Test
    void blockGrow_gatedByGrowthCategory() {
        assertCategoryGate(
                () -> new BlockGrowEvent(block(), mock(BlockState.class)),
                listener::onBlockGrow,
                PhysicsCategory.GROWTH);
    }

    @Test
    void blockSpread_gatedBySpreadingCategory() {
        assertCategoryGate(
                () -> new BlockSpreadEvent(block(), block(), mock(BlockState.class)),
                listener::onBlockSpread,
                PhysicsCategory.SPREADING);
    }

    @Test
    void blockPhysics_gatedByBlockUpdatesCategory() {
        assertCategoryGate(
                () -> {
                    Block block = block();
                    when(block.getBlockData()).thenReturn(mock(BlockData.class));
                    return new BlockPhysicsEvent(block, mock(BlockData.class));
                },
                listener::onBlockPhysics,
                PhysicsCategory.BLOCK_UPDATES);
    }

    @Test
    void blockPhysics_connectionsCategoryKeepsConnectableBlocksUpdating() {
        data.set(WorldDataKey.PHYSICS, false);
        Block fence = block();
        when(fence.getBlockData()).thenReturn(mock(Fence.class));

        BlockPhysicsEvent event = new BlockPhysicsEvent(fence, mock(BlockData.class));
        listener.onBlockPhysics(event);
        assertTrue(event.isCancelled(), "connections blocked must cancel fence updates");

        data.set(PhysicsCategory.CONNECTIONS.key(), true);
        event = new BlockPhysicsEvent(fence, mock(BlockData.class));
        listener.onBlockPhysics(event);
        assertFalse(event.isCancelled(), "re-allowed connections must keep fence updates");
    }

    @Test
    void blockFromTo_fluidFlowOnlyAppliesToLiquids() {
        data.set(WorldDataKey.PHYSICS, false);
        data.set(PhysicsCategory.FLUID_FLOW.key(), true);

        Block liquid = block();
        when(liquid.isLiquid()).thenReturn(true);
        BlockFromToEvent event = new BlockFromToEvent(liquid, BlockFace.DOWN);
        listener.onBlockFromTo(event);
        assertFalse(event.isCancelled(), "liquid flow must be allowed");

        // A non-liquid FromTo (e.g. a teleporting dragon egg) stays blocked even with fluid flow allowed.
        BlockFromToEvent solidEvent = new BlockFromToEvent(block(), BlockFace.DOWN);
        listener.onBlockFromTo(solidEvent);
        assertTrue(solidEvent.isCancelled(), "non-liquid FromTo must stay cancelled");
    }

    @Test
    void entityChangeBlock_gatedByFallingBlocksCategory() {
        data.set(WorldDataKey.PHYSICS, false);
        FallingBlock fallingBlock = mock(FallingBlock.class);
        when(fallingBlock.getType()).thenReturn(EntityType.FALLING_BLOCK);
        Block block = block();
        BlockState state = mock(BlockState.class);
        when(block.getState()).thenReturn(state);

        EntityChangeBlockEvent event = new EntityChangeBlockEvent(fallingBlock, block, mock(BlockData.class));
        listener.onEntityChangeBlock(event);
        assertTrue(event.isCancelled(), "falling blocks must be cancelled");
        verify(state).update(false, false);

        data.set(PhysicsCategory.FALLING_BLOCKS.key(), true);
        event = new EntityChangeBlockEvent(fallingBlock, block, mock(BlockData.class));
        listener.onEntityChangeBlock(event);
        assertFalse(event.isCancelled(), "re-allowed falling blocks must not be cancelled");
    }

    @Test
    void unmanagedWorld_isNeverTouched() {
        World unmanaged = mock(World.class);
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(unmanaged);

        LeavesDecayEvent event = new LeavesDecayEvent(block);
        listener.onLeavesDecay(event);
        assertFalse(event.isCancelled());
    }
}
