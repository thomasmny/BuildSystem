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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.VisibilityFixture;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class HidePlayersToggleTest {

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void turningHidePlayersOff_showsEveryoneButArchiveVanishedPlayers() {
        ServerMock server = MockBukkit.mock();
        VisibilityFixture fixture = new VisibilityFixture(MockBukkit.createMockPlugin());
        PlayerMock player = SoundlessPlayer.join(server, "Player");
        PlayerMock building = SoundlessPlayer.join(server, "Building");
        PlayerMock archived = SoundlessPlayer.join(server, "Archived");
        fixture.enterArchive(archived);
        SettingToggles toggles =
                new SettingToggles(fixture.settingsService, mock(NavigatorService.class), mock(NoClipService.class));

        fixture.hidePlayers(player, true);
        toggles.toggleHidePlayers(player);
        assertFalse(player.canSee(building));

        fixture.hidePlayers(player, false);
        toggles.toggleHidePlayers(player);
        assertTrue(player.canSee(building));
        assertFalse(player.canSee(archived));
    }
}
