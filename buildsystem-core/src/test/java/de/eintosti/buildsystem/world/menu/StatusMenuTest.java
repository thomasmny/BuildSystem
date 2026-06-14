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
package de.eintosti.buildsystem.world.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Golden test pinning the {@link StatusMenu} slot &rarr; status selection grid. Built through the package-private,
 * Bukkit-free constructor so no server is required.
 *
 * <p>Out of scope: executing a click that opens {@code EditMenu} or applies a status (constructs a {@code Menu} /
 * mutates world data via Bukkit); that needs {@code mockStatic(Bukkit.class)}, which this codebase avoids. The mapping
 * below is the regression net.
 */
@NullMarked
class StatusMenuTest {

    private static StatusMenu menu() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        BuildWorld buildWorld = mock(BuildWorld.class);
        Messages messages = mock(Messages.class);
        Inventory inventory = mock(Inventory.class);
        return new StatusMenu(plugin, buildWorld, messages, inventory);
    }

    @Test
    void statusBySlot_hasExactlySixEntriesMappedCorrectly() {
        Map<Integer, BuildWorldStatus> statuses = menu().statusBySlot();

        assertEquals(6, statuses.size());
        assertEquals(BuildWorldStatus.NOT_STARTED, statuses.get(10));
        assertEquals(BuildWorldStatus.IN_PROGRESS, statuses.get(11));
        assertEquals(BuildWorldStatus.ALMOST_FINISHED, statuses.get(12));
        assertEquals(BuildWorldStatus.FINISHED, statuses.get(13));
        assertEquals(BuildWorldStatus.ARCHIVE, statuses.get(14));
        assertEquals(BuildWorldStatus.HIDDEN, statuses.get(16));
    }

    @Test
    void statusBySlot_skipsSlotFifteen() {
        assertFalse(menu().statusBySlot().containsKey(15));
    }

    @Test
    void statusBySlot_fillerSlotIsAbsent() {
        assertNull(menu().statusBySlot().get(0));
    }
}
