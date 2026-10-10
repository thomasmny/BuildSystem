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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

@NullMarked
class DisabledInteractionsHandlerTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;
    private SettingInteractionListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();

        Settings settings = mock(Settings.class);
        when(settings.isDisableInteract()).thenReturn(true);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings(any())).thenReturn(settings);
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().builder().worldEditWand()).thenReturn(Material.WOODEN_AXE);

        listener = new SettingInteractionListener(settingsService, mock(WorldStorage.class), configService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void colouredBlockKeepsItsColour() {
        Block adjacent = clickChestWith(Material.RED_WOOL, BlockFace.NORTH);

        assertEquals(Material.RED_WOOL, adjacent.getType());
    }

    @Test
    void darkOakSignBecomesAWallSign() {
        Block adjacent = clickChestWith(Material.DARK_OAK_SIGN, BlockFace.NORTH);

        assertEquals(Material.DARK_OAK_WALL_SIGN, adjacent.getType());
    }

    @Test
    void interactionStaysDenied() {
        PlayerInteractEvent event = interact(Material.RED_WOOL, BlockFace.NORTH);

        listener.onInteract(event);

        assertTrue(event.isCancelled());
        assertEquals(Event.Result.DENY, event.useInteractedBlock());
    }

    @Test
    void cancelledPlacementIsReverted() {
        server.getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler
                            public void deny(BlockPlaceEvent event) {
                                event.setCancelled(true);
                            }
                        },
                        MockBukkit.createMockPlugin());

        Block adjacent = clickChestWith(Material.RED_WOOL, BlockFace.NORTH);

        assertEquals(Material.AIR, adjacent.getType());
    }

    private Block clickChestWith(Material held, BlockFace face) {
        PlayerInteractEvent event = interact(held, face);
        listener.onInteract(event);
        return event.getClickedBlock().getRelative(face);
    }

    private PlayerInteractEvent interact(Material held, BlockFace face) {
        Block chest = world.getBlockAt(0, 64, 0);
        chest.setType(Material.CHEST);
        return new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_BLOCK, new ItemStack(held), chest, face, EquipmentSlot.HAND);
    }
}
