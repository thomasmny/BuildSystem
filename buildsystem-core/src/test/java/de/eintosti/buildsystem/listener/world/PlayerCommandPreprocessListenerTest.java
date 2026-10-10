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
package de.eintosti.buildsystem.listener.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Pins the restricted WorldEdit command block: which players it stops in a BuildSystem world, and with which message.
 */
@NullMarked
class PlayerCommandPreprocessListenerTest {

    private Messages messages;
    private WorldStorage worldStorage;
    private WorldPermissions permissions;
    private SoundlessPlayer player;
    private PlayerCommandPreprocessListener listener;

    @BeforeEach
    void setUp() {
        player = SoundlessPlayer.join(MockBukkit.mock(), "Alex");
        messages = mock(Messages.class);
        worldStorage = mock(WorldStorage.class);
        permissions = mock(WorldPermissions.class);
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().builder().blockWorldEditNonBuilder())
                .thenReturn(true);
        listener = new PlayerCommandPreprocessListener(
                mock(SettingsService.class),
                worldStorage,
                mock(NavigatorItems.class),
                configService,
                messages,
                mock(TaskScheduler.class));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void archivedWorld_blocksAPlainPlayer() {
        world(WorldDataSchema.create("world", TestData.ARCHIVE_STATUS));

        assertTrue(runSet().isCancelled());
        verify(messages).sendMessage(player, "command_archive_world");
    }

    @Test
    void buildersEnabled_blocksANonBuilder() {
        world(buildersEnabled());

        assertTrue(runSet().isCancelled());
        verify(messages).sendMessage(player, "command_not_builder");
    }

    @Test
    void anAdmin_passes() {
        world(buildersEnabled());
        when(permissions.hasAdminPermission(player)).thenReturn(true);

        assertFalse(runSet().isCancelled());
        verify(messages, never()).sendMessage(any(), anyString());
    }

    @Test
    void aWorldBuildSystemDoesNotManage_passes() {
        assertFalse(runSet().isCancelled());
        verify(messages, never()).sendMessage(any(), anyString());
    }

    private static WorldDataImpl buildersEnabled() {
        WorldDataImpl data = WorldDataSchema.create("world", TestData.NOT_STARTED);
        data.set(WorldDataKey.BUILDERS_ENABLED, true);
        return data;
    }

    private void world(WorldDataImpl data) {
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getPermissions()).thenReturn(permissions);
        when(buildWorld.getBuilders()).thenReturn(mock(Builders.class));
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);
    }

    private PlayerCommandPreprocessEvent runSet() {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "//set stone");
        listener.onPlayerCommandPreprocess(event);
        return event;
    }
}
