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
package de.eintosti.buildsystem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MaterialUtilsTest {

    @Test
    void wallVariant_mapsStandingAndHangingSigns() {
        assertEquals(Material.OAK_WALL_SIGN, MaterialUtils.wallVariant(Material.OAK_SIGN));
        assertEquals(Material.DARK_OAK_WALL_SIGN, MaterialUtils.wallVariant(Material.DARK_OAK_SIGN));
        assertEquals(Material.PALE_OAK_WALL_HANGING_SIGN, MaterialUtils.wallVariant(Material.PALE_OAK_HANGING_SIGN));
    }

    @Test
    void wallVariant_rejectsEverythingElse() {
        assertNull(MaterialUtils.wallVariant(Material.STONE));
        assertNull(MaterialUtils.wallVariant(Material.OAK_WALL_SIGN));
        assertNull(MaterialUtils.wallVariant(Material.OAK_WALL_HANGING_SIGN));
    }
}
