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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.world.menu.EditMenu.ClickOutcome;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Golden test pinning the {@link EditMenu} slot &rarr; permission and slot &rarr; {@link ClickOutcome} contract. The
 * menu is built through its real production constructor under a {@link MockBukkit} server, so no test-only seam is
 * required.
 */
@NullMarked
class EditMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private EditMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getPlayerService()).thenReturn(mock(PlayerServiceImpl.class));
        BuildWorld buildWorld = mock(BuildWorld.class);
        Player player = server.addPlayer();
        return new EditMenu(plugin, buildWorld, player);
    }

    @Test
    void permissionBySlot_mapsEveryInteractiveSlotToItsPermission() {
        Map<Integer, String> permissions = menu().permissionBySlot();

        // Toggles
        assertEquals("buildsystem.edit.pin", permissions.get(5));
        assertEquals("buildsystem.edit.breaking", permissions.get(20));
        assertEquals("buildsystem.edit.placement", permissions.get(21));
        assertEquals("buildsystem.edit.physics", permissions.get(22));
        assertEquals("buildsystem.edit.explosions", permissions.get(24));
        assertEquals("buildsystem.edit.mobai", permissions.get(31));
        assertEquals("buildsystem.edit.interactions", permissions.get(33));

        // Heterogeneous slots
        assertEquals("buildsystem.edit.time", permissions.get(23));
        assertEquals("buildsystem.edit.entities", permissions.get(29));
        assertEquals("buildsystem.edit.builders", permissions.get(30));
        assertEquals("buildsystem.edit.visibility", permissions.get(32));
        assertEquals("buildsystem.edit.gamerules", permissions.get(38));
        assertEquals("buildsystem.edit.difficulty", permissions.get(39));
        assertEquals("buildsystem.edit.status", permissions.get(40));
        assertEquals("buildsystem.edit.project", permissions.get(41));
        assertEquals("buildsystem.edit.permission", permissions.get(42));

        assertEquals(16, permissions.size());
    }

    @Test
    void permissionBySlot_omitsRenderOnlyWorldInfoSlot() {
        assertFalse(menu().permissionBySlot().containsKey(3));
    }

    @Test
    void outcomeBySlot_classifiesEachSlot() {
        Map<Integer, ClickOutcome> outcomes = menu().outcomeBySlot();

        // Render-only world-info slot
        assertEquals(ClickOutcome.NONE, outcomes.get(3));

        // Toggles all re-open
        assertEquals(ClickOutcome.REOPEN, outcomes.get(5));
        assertEquals(ClickOutcome.REOPEN, outcomes.get(20));
        assertEquals(ClickOutcome.REOPEN, outcomes.get(21));
        assertEquals(ClickOutcome.REOPEN, outcomes.get(22));
        assertEquals(ClickOutcome.REOPEN, outcomes.get(24));
        assertEquals(ClickOutcome.REOPEN, outcomes.get(31));
        assertEquals(ClickOutcome.REOPEN, outcomes.get(33));

        // Re-open actions
        assertEquals(ClickOutcome.REOPEN, outcomes.get(23)); // time
        assertEquals(ClickOutcome.REOPEN, outcomes.get(30)); // builders (left-click toggle)
        assertEquals(ClickOutcome.REOPEN, outcomes.get(32)); // visibility
        assertEquals(ClickOutcome.REOPEN, outcomes.get(39)); // difficulty

        // Sub-menus
        assertEquals(ClickOutcome.SUBMENU, outcomes.get(38)); // gamerules
        assertEquals(ClickOutcome.SUBMENU, outcomes.get(40)); // status

        // Chat input
        assertEquals(ClickOutcome.INPUT, outcomes.get(41)); // project
        assertEquals(ClickOutcome.INPUT, outcomes.get(42)); // permission

        // Closes the inventory
        assertEquals(ClickOutcome.CLOSE, outcomes.get(29)); // butcher
    }
}
