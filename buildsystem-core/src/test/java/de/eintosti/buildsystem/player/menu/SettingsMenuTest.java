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

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.menu.SettingsMenu.ClickOutcome;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Golden test pinning the {@link SettingsMenu} slot &rarr; permission-node mapping and the design/scoreboard
 * classifications. Built through the real production constructor under a {@link MockBukkit} server.
 */
@NullMarked
class SettingsMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private SettingsMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        return new SettingsMenu(
                messages,
                mock(SettingsService.class),
                mock(ConfigService.class),
                mock(MenuItems.class),
                mock(NavigatorService.class),
                mock(NoClipService.class),
                mock(Menus.class),
                server.addPlayer());
    }

    @Test
    void permissionNodeBySlot_mapsAllThirteenToggles() {
        Map<Integer, String> nodes = menu().permissionNodeBySlot();

        assertEquals("buildsystem.setting.clear-inventory", nodes.get(12));
        assertEquals("buildsystem.setting.disable-interact", nodes.get(13));
        assertEquals("buildsystem.setting.hide-players", nodes.get(14));
        assertEquals("buildsystem.setting.instant-place-signs", nodes.get(15));
        assertEquals("buildsystem.setting.keep-navigator", nodes.get(20));
        assertEquals("buildsystem.setting.navigator-type", nodes.get(21));
        assertEquals("buildsystem.setting.night-vision", nodes.get(22));
        assertEquals("buildsystem.setting.no-clip", nodes.get(23));
        assertEquals("buildsystem.setting.open-trapdoors", nodes.get(24));
        assertEquals("buildsystem.setting.place-plants", nodes.get(29));
        assertEquals("buildsystem.setting.scoreboard", nodes.get(30));
        assertEquals("buildsystem.setting.slab-breaking", nodes.get(31));
        assertEquals("buildsystem.setting.spawn-teleport", nodes.get(32));

        assertEquals(13, nodes.size());
    }

    @Test
    void permissionNodeBySlot_omitsDesignSlot() {
        assertFalse(menu().permissionNodeBySlot().containsKey(11));
    }

    @Test
    void outcomeBySlot_designIsSubmenu_scoreboardIsRejectable_restAreToggles() {
        Map<Integer, ClickOutcome> outcomes = menu().outcomeBySlot();

        assertEquals(ClickOutcome.SUBMENU, outcomes.get(11)); // design
        assertEquals(ClickOutcome.REJECTABLE, outcomes.get(30)); // scoreboard

        for (int slot : new int[] {12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 31, 32}) {
            assertEquals(ClickOutcome.TOGGLE, outcomes.get(slot), "slot " + slot + " should be a plain toggle");
        }
    }
}
