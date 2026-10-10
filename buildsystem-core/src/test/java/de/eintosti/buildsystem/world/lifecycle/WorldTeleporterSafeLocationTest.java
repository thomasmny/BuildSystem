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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class WorldTeleporterSafeLocationTest {

    @Test
    void caveAirCountsAsFreeSpace() {
        assertTrue(WorldTeleporterImpl.isSafeLocation(location(Material.CAVE_AIR, Material.VOID_AIR, Material.STONE)));
    }

    @Test
    void solidHeadIsUnsafe() {
        assertFalse(WorldTeleporterImpl.isSafeLocation(location(Material.AIR, Material.STONE, Material.STONE)));
    }

    @Test
    void airGroundIsUnsafe() {
        assertFalse(WorldTeleporterImpl.isSafeLocation(location(Material.AIR, Material.AIR, Material.CAVE_AIR)));
    }

    private static Location location(Material feet, Material head, Material ground) {
        Block feetBlock = block(feet);
        Block headBlock = block(head);
        Block groundBlock = block(ground);
        when(feetBlock.getRelative(BlockFace.UP)).thenReturn(headBlock);
        when(feetBlock.getRelative(BlockFace.DOWN)).thenReturn(groundBlock);

        Location above = mock(Location.class);
        when(above.getBlock()).thenReturn(headBlock);
        Location feetLocation = mock(Location.class);
        when(feetLocation.add(0, 1, 0)).thenReturn(above);
        when(feetBlock.getLocation()).thenReturn(feetLocation);

        Location location = mock(Location.class);
        when(location.getBlock()).thenReturn(feetBlock);
        return location;
    }

    private static Block block(Material type) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        return block;
    }
}
