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
package de.eintosti.buildsystem.player.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.menu.SpeedMenu.SpeedOption;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Golden test pinning the {@link SpeedMenu} slot &rarr; speed selection grid. Built through the real production
 * constructor under a {@link MockBukkit} server.
 */
@NullMarked
class SpeedMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private SpeedMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getSettingsService()).thenReturn(mock(SettingsService.class));
        Player player = server.addPlayer();
        return new SpeedMenu(plugin, player);
    }

    @Test
    void speedBySlot_hasFiveEntriesMappedToTheirSpeeds() {
        Map<Integer, SpeedOption> speeds = menu().speedBySlot();

        assertEquals(5, speeds.size());
        assertSpeed(speeds.get(11), 0.2f, 1);
        assertSpeed(speeds.get(12), 0.4f, 2);
        assertSpeed(speeds.get(13), 0.6f, 3);
        assertSpeed(speeds.get(14), 0.8f, 4);
        assertSpeed(speeds.get(15), 1.0f, 5);
    }

    @Test
    void speedBySlot_fillerSlotIsAbsent() {
        assertFalse(menu().speedBySlot().containsKey(0));
    }

    private static void assertSpeed(SpeedOption option, float expectedSpeed, int expectedDisplayNumber) {
        assertEquals(expectedSpeed, option.speed());
        assertEquals(expectedDisplayNumber, option.displayNumber());
    }
}
