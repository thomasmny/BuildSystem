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
package de.eintosti.buildsystem.listener.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.TestData;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * The per-player build settings that act on a block click, run through the server's event bus as they are registered
 * in production: one shared protection gate, and the first setting that takes the click wins.
 */
@NullMarked
class SettingInteractionListenerTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private Settings settings;
    private WorldStorage worldStorage;
    private final AtomicInteger placeEvents = new AtomicInteger();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        // MockBukkit does not ray trace; null means the player is not aiming at the top half.
        player = new PlayerMock(server, "Alex") {
            @Override
            public @Nullable RayTraceResult rayTraceBlocks(double maxDistance) {
                return null;
            }
        };
        server.addPlayer(player);
        player.teleport(world.getSpawnLocation());

        settings = mock(Settings.class);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings(any())).thenReturn(settings);
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().builder().worldEditWand()).thenReturn(XMaterial.WOODEN_AXE);
        worldStorage = mock(WorldStorage.class);

        Plugin plugin = MockBukkit.createMockPlugin();
        server.getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler
                            public void count(BlockPlaceEvent event) {
                                placeEvents.incrementAndGet();
                            }
                        },
                        plugin);
        server.getPluginManager()
                .registerEvents(new SettingInteractionListener(settingsService, worldStorage, configService), plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void protectedWorld_leavesTheClickAlone() {
        when(settings.isPlacePlants()).thenReturn(true);
        lockedBuildWorld();
        Block grass = block(Material.GRASS_BLOCK);

        PlayerInteractEvent event = rightClick(grass, BlockFace.UP, Material.FERN);

        assertFalse(event.isCancelled());
        assertEquals(Material.AIR, grass.getRelative(BlockFace.UP).getType());
    }

    @Test
    void placePlants_placesThePlant() {
        when(settings.isPlacePlants()).thenReturn(true);
        Block grass = block(Material.GRASS_BLOCK);

        PlayerInteractEvent event = rightClick(grass, BlockFace.UP, Material.FERN);

        assertTrue(event.isCancelled());
        assertEquals(Material.FERN, grass.getRelative(BlockFace.UP).getType());
    }

    @Test
    void instantSigns_placesTheSign() {
        when(settings.isInstantPlaceSigns()).thenReturn(true);
        Block stone = block(Material.STONE);

        rightClick(stone, BlockFace.NORTH, Material.OAK_SIGN);

        assertEquals(Material.OAK_WALL_SIGN, stone.getRelative(BlockFace.NORTH).getType());
        assertEquals(0, placeEvents.get());
    }

    @Test
    void disabledInteractions_goesBeforeInstantSigns() {
        when(settings.isDisableInteract()).thenReturn(true);
        when(settings.isInstantPlaceSigns()).thenReturn(true);
        Block chest = block(Material.CHEST);

        rightClick(chest, BlockFace.NORTH, Material.OAK_SIGN);

        assertEquals(Material.OAK_WALL_SIGN, chest.getRelative(BlockFace.NORTH).getType());
        assertEquals(1, placeEvents.get(), "only disabled interactions fires a BlockPlaceEvent");
    }

    @Test
    void slabBreaking_removesOneHalf() {
        when(settings.isSlabBreaking()).thenReturn(true);
        Block block = block(Material.STONE_SLAB);
        Slab slab = (Slab) block.getBlockData();
        slab.setType(Slab.Type.DOUBLE);
        block.setBlockData(slab);

        PlayerInteractEvent event =
                new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertEquals(Slab.Type.TOP, ((Slab) block.getBlockData()).getType());
    }

    @Test
    void settingOff_doesNothing() {
        Block grass = block(Material.GRASS_BLOCK);

        PlayerInteractEvent event = rightClick(grass, BlockFace.UP, Material.FERN);

        assertFalse(event.isCancelled());
        assertEquals(Material.AIR, grass.getRelative(BlockFace.UP).getType());
    }

    @Test
    void ironDoors_togglesAnIronTrapdoor() {
        when(settings.isOpenTrapDoors()).thenReturn(true);
        Block trapdoor = block(Material.IRON_TRAPDOOR);

        PlayerInteractEvent event = rightClick(trapdoor, BlockFace.UP, null);

        assertTrue(event.isCancelled());
        assertTrue(((Openable) trapdoor.getBlockData()).isOpen());
    }

    @Test
    void alreadyCancelledClick_isIgnored() {
        when(settings.isPlacePlants()).thenReturn(true);
        Block grass = block(Material.GRASS_BLOCK);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.FERN),
                grass,
                BlockFace.UP,
                EquipmentSlot.HAND);
        event.setCancelled(true);

        server.getPluginManager().callEvent(event);

        assertEquals(Material.AIR, grass.getRelative(BlockFace.UP).getType());
    }

    @Test
    void openBuildWorld_placesThePlant() {
        when(settings.isPlacePlants()).thenReturn(true);
        buildWorld(TestData.NOT_STARTED, true, false);
        Block grass = block(Material.GRASS_BLOCK);

        rightClick(grass, BlockFace.UP, Material.FERN);

        assertEquals(Material.FERN, grass.getRelative(BlockFace.UP).getType());
    }

    @Test
    void lockedBuildWorld_letsAnAdminPlaceThePlant() {
        when(settings.isPlacePlants()).thenReturn(true);
        WorldPermissions permissions = lockedBuildWorld();
        when(permissions.hasAdminPermission(player)).thenReturn(true);
        Block grass = block(Material.GRASS_BLOCK);

        rightClick(grass, BlockFace.UP, Material.FERN);

        assertEquals(Material.FERN, grass.getRelative(BlockFace.UP).getType());
    }

    @Test
    void placementOffAndNotABuilder_letsTheSettingsBypassPlaceThePlant() {
        when(settings.isPlacePlants()).thenReturn(true);
        buildWorld(TestData.NOT_STARTED, false, true);
        player.addAttachment(MockBukkit.createMockPlugin(), "buildsystem.bypass.settings", true);
        Block grass = block(Material.GRASS_BLOCK);

        rightClick(grass, BlockFace.UP, Material.FERN);

        assertEquals(Material.FERN, grass.getRelative(BlockFace.UP).getType());
    }

    private WorldPermissions lockedBuildWorld() {
        return buildWorld(TestData.ARCHIVE_STATUS, true, false);
    }

    private WorldPermissions buildWorld(BuildWorldStatus status, boolean placement, boolean buildersEnabled) {
        WorldPermissions permissions = mock(WorldPermissions.class);
        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(status);
        when(data.get(WorldDataKey.BLOCK_PLACEMENT)).thenReturn(placement);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(buildersEnabled);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getPermissions()).thenReturn(permissions);
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getBuilders()).thenReturn(mock(Builders.class));
        when(worldStorage.getBuildWorld(world)).thenReturn(buildWorld);
        return permissions;
    }

    private Block block(Material type) {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(type);
        return block;
    }

    private PlayerInteractEvent rightClick(Block block, BlockFace face, @Nullable Material held) {
        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                held == null ? null : new ItemStack(held),
                block,
                face,
                EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
