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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Golden test pinning the {@link StatusMenu} slot &rarr; status selection grid. Built through the real production
 * constructor under a {@link MockBukkit} server.
 */
@NullMarked
class StatusMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private StatusMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any(), any())).thenReturn("Title");
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getMessages()).thenReturn(messages);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn("world");
        Player player = server.addPlayer();
        return new StatusMenu(plugin, buildWorld, player);
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
