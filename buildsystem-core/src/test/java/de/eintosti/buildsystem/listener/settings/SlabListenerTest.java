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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@NullMarked
class SlabListenerTest {

    @ParameterizedTest
    @CsvSource({
        "64, 65.0, true", // top face
        "64, 64.7, true",
        "64, 64.2, false",
        "64, 64.0, false", // bottom face
        "-10, -9.0, true",
        "-10, -9.2, true",
        "-10, -9.8, false",
        "-1, -0.3, true",
        "-1, -0.7, false"
    })
    void hitsTopHalf_measuresFromTheBlock(int blockY, double hitY, boolean expected) {
        Block block = mock(Block.class);
        when(block.getY()).thenReturn(blockY);
        Player player = mock(Player.class);
        when(player.rayTraceBlocks(6)).thenReturn(new RayTraceResult(new Vector(0.5, hitY, 0.5), BlockFace.NORTH));

        assertEquals(expected, SlabListener.hitsTopHalf(player, block));
    }
}
