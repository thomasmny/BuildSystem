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
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.test.VisibilityFixture;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

@NullMarked
class PlayerChangedWorldListenerTest {

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void leavingAnArchiveForABuildableWorld_showsThePlayer_exceptToHidePlayersViewers() {
        ServerMock server = MockBukkit.mock();
        WorldMock from = server.addSimpleWorld("from");
        WorldMock to = server.addSimpleWorld("to");
        VisibilityFixture fixture = new VisibilityFixture(MockBukkit.createMockPlugin());
        PlayerMock viewer = SoundlessPlayer.join(server, "Viewer");
        PlayerMock bystander = SoundlessPlayer.join(server, "Bystander");
        PlayerMock target = SoundlessPlayer.join(server, "Target");
        fixture.hidePlayers(viewer, true);
        fixture.enterArchive(target);
        fixture.settingsService.updateVisibility(target);
        assertFalse(bystander.canSee(target));
        target.teleport(to.getSpawnLocation());

        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.NOT_STARTED);
        when(data.get(WorldDataKey.PHYSICS)).thenReturn(true);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getData()).thenReturn(data);
        WorldStorage worldStorage = mock(WorldStorage.class);
        when(worldStorage.getBuildWorld(to)).thenReturn(buildWorld);

        PlayerChangedWorldListener listener = new PlayerChangedWorldListener(
                mock(NavigatorService.class),
                fixture.playerService,
                fixture.settingsService,
                worldStorage,
                fixture.configService,
                mock(Messages.class));
        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(target, from));

        assertTrue(bystander.canSee(target));
        assertFalse(viewer.canSee(target));
    }
}
