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
package de.eintosti.buildsystem.util.inventory;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class PaginatedInventoryTest {

    private PaginatedInventory inventory;

    @BeforeEach
    void setUp() {
        // PaginatedInventory is abstract; InventoryHandler has only default methods.
        // Anonymous subclass requires no additional implementation.
        inventory = new PaginatedInventory() {};
    }

    @Test
    void calculateNumPages_zeroObjects_returnsOne() {
        assertEquals(1, inventory.calculateNumPages(0, 9));
    }

    @Test
    void calculateNumPages_exactlyOnePage_returnsOne() {
        assertEquals(1, inventory.calculateNumPages(9, 9));
    }

    @Test
    void calculateNumPages_oneOverPage_returnsTwo() {
        assertEquals(2, inventory.calculateNumPages(10, 9));
    }

    @Test
    void calculateNumPages_exactlyThreePages_returnsThree() {
        assertEquals(3, inventory.calculateNumPages(27, 9));
    }

    @Test
    void calculateNumPages_oneObject_returnsOne() {
        assertEquals(1, inventory.calculateNumPages(1, 9));
    }
}
