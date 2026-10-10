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
package de.eintosti.buildsystem.world.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * The callers pass the block a player would stand on and teleport them one block above it, so that block must be solid
 * and the two above it free.
 */
@NullMarked
class WorldTeleporterImplTest {

    private WorldMock world;

    @BeforeEach
    void setUp() {
        world = MockBukkit.mock().addSimpleWorld("nether");
        for (int y = 50; y <= 70; y++) {
            world.getBlockAt(0, y, 0).setType(Material.AIR);
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void solidBlockWithRoomAbove_isSafe() {
        world.getBlockAt(0, 60, 0).setType(Material.NETHERRACK);

        assertTrue(WorldTeleporterImpl.isSafeLocation(new Location(world, 0, 60, 0)));
    }

    @Test
    void caveAirAbove_countsAsRoom() {
        world.getBlockAt(0, 60, 0).setType(Material.NETHERRACK);
        world.getBlockAt(0, 61, 0).setType(Material.CAVE_AIR);
        world.getBlockAt(0, 62, 0).setType(Material.VOID_AIR);

        assertTrue(WorldTeleporterImpl.isSafeLocation(new Location(world, 0, 60, 0)));
    }

    @Test
    void solidBlockWithAnotherBlockAtHeadHeight_isNotSafe() {
        world.getBlockAt(0, 59, 0).setType(Material.NETHERRACK);
        world.getBlockAt(0, 60, 0).setType(Material.NETHERRACK);
        world.getBlockAt(0, 62, 0).setType(Material.NETHERRACK);

        assertFalse(WorldTeleporterImpl.isSafeLocation(new Location(world, 0, 60, 0)));
    }

    @Test
    void solidBlockWithAnotherBlockAtFeetHeight_isNotSafe() {
        world.getBlockAt(0, 59, 0).setType(Material.NETHERRACK);
        world.getBlockAt(0, 60, 0).setType(Material.NETHERRACK);
        world.getBlockAt(0, 61, 0).setType(Material.NETHERRACK);

        assertFalse(WorldTeleporterImpl.isSafeLocation(new Location(world, 0, 60, 0)));
    }

    @Test
    void airWithGroundBelow_isNotSafe() {
        world.getBlockAt(0, 59, 0).setType(Material.NETHERRACK);

        assertFalse(
                WorldTeleporterImpl.isSafeLocation(new Location(world, 0, 60, 0)),
                "the player would land one block above, in mid-air");
    }
}
