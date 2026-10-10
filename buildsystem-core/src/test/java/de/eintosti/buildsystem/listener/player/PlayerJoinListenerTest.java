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
package de.eintosti.buildsystem.listener.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.VisibilityFixture;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class PlayerJoinListenerTest {

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joiningPlayer_doesNotSeeAnArchiveVanishedPlayer() {
        ServerMock server = MockBukkit.mock();
        server.addSimpleWorld("world");
        VisibilityFixture fixture = new VisibilityFixture(MockBukkit.createMockPlugin());
        PlayerMock archived = SoundlessPlayer.join(server, "Archived");
        PlayerMock building = SoundlessPlayer.join(server, "Building");
        fixture.enterArchive(archived);
        PlayerMock joiner = SoundlessPlayer.join(server, "Joiner");

        PlayerJoinListener listener = new PlayerJoinListener(
                fixture.playerService,
                fixture.settingsService,
                mock(NavigatorService.class),
                mock(SpawnService.class),
                mock(WorldStorage.class),
                mock(PlayerLookupService.class),
                mock(NoClipService.class),
                fixture.configService,
                mock(Messages.class),
                mock(UpdateChecker.class),
                mock(TaskScheduler.class));
        listener.onPlayerJoin(new PlayerJoinEvent(joiner, "joined"));

        assertFalse(joiner.canSee(archived));
        assertTrue(joiner.canSee(building));
    }
}
