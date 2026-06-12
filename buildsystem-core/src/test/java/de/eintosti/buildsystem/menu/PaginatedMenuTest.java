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
package de.eintosti.buildsystem.menu;

import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@NullMarked
class PaginatedMenuTest {

    private static final int ITEMS_PER_PAGE = 9;

    private static TestMenu menu(int totalItems) {
        Messages messages = mock(Messages.class);
        Inventory inventory = mock(Inventory.class);
        return new TestMenu(messages, inventory, totalItems);
    }

    // Concrete subclass — sound is a no-op so XSound does not reach the Bukkit server
    private static class TestMenu extends PaginatedMenu {

        private final int total;

        TestMenu(Messages messages, Inventory inventory, int total) {
            super(messages, inventory);
            this.total = total;
        }

        @Override
        protected int totalItems() {
            return total;
        }

        @Override
        protected void populate(Player player) {}

        @Override
        protected void playPageSound(Player player) {}

        @Override
        protected void playRefuseSound(Player player) {}
    }

    @Test
    void pageStartsAtZero() {
        assertEquals(0, menu(0).page());
    }

    @Test
    void totalPages_zeroItems_returnsOne() {
        assertEquals(1, menu(0).totalPages(ITEMS_PER_PAGE));
    }

    @Test
    void totalPages_exactlyOnePage_returnsOne() {
        assertEquals(1, menu(9).totalPages(ITEMS_PER_PAGE));
    }

    @Test
    void totalPages_oneOverPage_returnsTwo() {
        assertEquals(2, menu(10).totalPages(ITEMS_PER_PAGE));
    }

    @Test
    void totalPages_exactlyThreePages_returnsThree() {
        assertEquals(3, menu(27).totalPages(ITEMS_PER_PAGE));
    }

    @Test
    void previousPage_atFirstPage_returnsFalse() {
        TestMenu m = menu(18);
        Player player = mock(Player.class);
        assertFalse(m.previousPage(player, ITEMS_PER_PAGE));
        assertEquals(0, m.page());
    }

    @Test
    void nextPage_atLastPage_returnsFalse() {
        TestMenu m = menu(18);
        Player player = mock(Player.class);
        m.nextPage(player, ITEMS_PER_PAGE); // advance to page 1 (last)
        assertFalse(m.nextPage(player, ITEMS_PER_PAGE));
        assertEquals(1, m.page());
    }

    @Test
    void nextThenPrevious_cyclesCorrectly() {
        TestMenu m = menu(18);
        Player player = mock(Player.class);
        assertTrue(m.nextPage(player, ITEMS_PER_PAGE));
        assertEquals(1, m.page());
        assertTrue(m.previousPage(player, ITEMS_PER_PAGE));
        assertEquals(0, m.page());
    }

    @Test
    void previousPage_singlePage_returnsFalse() {
        TestMenu m = menu(5);
        Player player = mock(Player.class);
        assertFalse(m.previousPage(player, ITEMS_PER_PAGE));
    }

    @Test
    void nextPage_singlePage_returnsFalse() {
        TestMenu m = menu(5);
        Player player = mock(Player.class);
        assertFalse(m.nextPage(player, ITEMS_PER_PAGE));
    }
}
