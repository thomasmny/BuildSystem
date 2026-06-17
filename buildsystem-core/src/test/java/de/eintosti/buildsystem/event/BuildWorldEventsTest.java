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
package de.eintosti.buildsystem.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.api.event.world.BuildWorldCreateEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldRenameEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldStatusChangeEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.test.TestData;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class BuildWorldEventsTest {

    @Test
    void createEvent_gettersReturnConstructorValues() {
        BuildWorldCreateEvent event = new BuildWorldCreateEvent("MyWorld", BuildWorldType.NORMAL, null, true);

        assertEquals("MyWorld", event.getWorldName());
        assertEquals(BuildWorldType.NORMAL, event.getType());
        assertNull(event.getCreator());
        assertTrue(event.isImport());
    }

    @Test
    void createEvent_cancellation() {
        BuildWorldCreateEvent event = new BuildWorldCreateEvent("MyWorld", BuildWorldType.NORMAL, null, false);

        assertFalse(event.isImport());
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void renameEvent_returnsOldAndNewNames() {
        BuildWorld buildWorld = mock(BuildWorld.class);
        BuildWorldRenameEvent event = new BuildWorldRenameEvent(buildWorld, "OldName", "NewName");

        assertSame(buildWorld, event.getBuildWorld());
        assertEquals("OldName", event.getOldName());
        assertEquals("NewName", event.getNewName());
    }

    @Test
    void statusChangeEvent_returnsPreviousAndNewStatus() {
        BuildWorld buildWorld = mock(BuildWorld.class);
        BuildWorldStatusChangeEvent event =
                new BuildWorldStatusChangeEvent(buildWorld, TestData.NOT_STARTED, TestData.FINISHED);

        assertSame(buildWorld, event.getBuildWorld());
        assertEquals(TestData.NOT_STARTED, event.getPreviousStatus());
        assertEquals(TestData.FINISHED, event.getNewStatus());
    }
}
