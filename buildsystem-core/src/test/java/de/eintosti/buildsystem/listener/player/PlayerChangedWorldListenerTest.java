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

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.test.TestData;
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
    void changingIntoABuildableWorld_updatesWhoSeesThePlayer() {
        ServerMock server = MockBukkit.mock();
        WorldMock from = server.addSimpleWorld("from");
        WorldMock to = server.addSimpleWorld("to");
        PlayerMock target = server.addPlayer("Target");
        target.teleport(to.getSpawnLocation());

        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getSettings(target)).thenReturn(new SettingsImpl());

        PlayerStorageImpl playerStorage = mock(PlayerStorageImpl.class);
        when(playerStorage.getBuildPlayer(target))
                .thenReturn(new BuildPlayerImpl(target.getUniqueId(), new SettingsImpl()));
        PlayerServiceImpl playerService = mock(PlayerServiceImpl.class);
        when(playerService.getPlayerStorage()).thenReturn(playerStorage);

        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.NOT_STARTED);
        when(data.get(WorldDataKey.PHYSICS)).thenReturn(true);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getData()).thenReturn(data);
        WorldStorage worldStorage = mock(WorldStorage.class);
        when(worldStorage.getBuildWorld(to)).thenReturn(buildWorld);

        PlayerChangedWorldListener listener = new PlayerChangedWorldListener(
                mock(NavigatorService.class),
                playerService,
                settingsService,
                worldStorage,
                mock(ConfigService.class, RETURNS_DEEP_STUBS),
                mock(Messages.class));
        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(target, from));

        verify(settingsService).updateVisibility(target);
    }
}
