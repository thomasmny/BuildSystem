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
package de.eintosti.buildsystem.integration.worldedit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.world.World;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.TaskScheduler;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.ArgumentCaptor;

@NullMarked
class EditSessionListenerTest {

    private ServerMock server;
    private WorldStorageImpl worldStorage;
    private TaskScheduler scheduler;
    private EditSessionListener listener;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        worldStorage = mock(WorldStorageImpl.class);
        scheduler = mock(TaskScheduler.class);
        listener = new EditSessionListener(worldStorage, scheduler);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void judgesTheEditedWorldRatherThanThePlayersWorld() {
        WorldMock standing = server.addSimpleWorld("standing");
        WorldMock edited = server.addSimpleWorld("edited");
        player.teleport(standing.getSpawnLocation());
        BuildWorld open = buildWorld(true);
        when(worldStorage.getBuildWorld(standing)).thenReturn(open);
        BuildWorld archived = buildWorld(false);
        when(worldStorage.getBuildWorld(edited)).thenReturn(archived);

        EditSessionEvent event = event("edited");
        listener.onEditSession(event);

        assertInstanceOf(NullExtent.class, event.getExtent());
        verify(scheduler, never()).run(any());
    }

    @Test
    void recordsTheEditOnTheMainThread() {
        server.addSimpleWorld("edited");
        BuildWorld open = buildWorld(true);
        when(worldStorage.getBuildWorld(any(WorldMock.class))).thenReturn(open);

        EditSessionEvent event = event("edited");
        listener.onEditSession(event);

        assertFalse(event.getExtent() instanceof NullExtent);
        verify(open.getData(), never()).set(eq(WorldDataKey.LAST_EDITED), anyLong());
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).run(task.capture());
        task.getValue().run();
        verify(open.getData()).set(eq(WorldDataKey.LAST_EDITED), anyLong());
    }

    private EditSessionEvent event(String worldName) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        Actor actor = mock(Actor.class);
        when(actor.isPlayer()).thenReturn(true);
        when(actor.getUniqueId()).thenReturn(player.getUniqueId());
        EditSessionEvent event = new EditSessionEvent(world, actor, -1, EditSession.Stage.BEFORE_CHANGE);
        assertNotNull(event.getActor());
        return event;
    }

    private static BuildWorld buildWorld(boolean buildingAllowed) {
        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS))
                .thenReturn(buildingAllowed ? TestData.IN_PROGRESS : TestData.ARCHIVE_STATUS);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(false);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getPermissions()).thenReturn(mock(WorldPermissions.class));
        when(buildWorld.getBuilders()).thenReturn(mock(Builders.class));
        return buildWorld;
    }
}
