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
package de.eintosti.buildsystem.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import de.eintosti.buildsystem.api.event.folder.FolderCreatedEvent;
import de.eintosti.buildsystem.api.event.folder.FolderDeletedEvent;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.yaml.YamlEntityFile;
import de.eintosti.buildsystem.test.TestData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.event.Event;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@NullMarked
class FolderStorageImplTest {

    private FolderStorageImpl storage;
    private final List<Event> firedEvents = new ArrayList<>();
    private final List<Folder> deletedFromStorage = new ArrayList<>();
    private Builder creator;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger(FolderStorageImplTest.class.getName());
        WorldStorage worldStorage = Mockito.mock(WorldStorage.class);
        creator = Builder.of(UUID.randomUUID(), "TestPlayer");

        YamlEntityFile<Folder> file = TestData.noopEntityFile();
        doAnswer(invocation -> {
                    deletedFromStorage.add(invocation.getArgument(0));
                    return CompletableFuture.completedFuture(null);
                })
                .when(file)
                .delete(any(Folder.class));
        storage = new FolderStorageImpl(logger, worldStorage, TestData::worldContext, file) {
            @Override
            protected void fireEvent(Event event) {
                firedEvents.add(event);
            }
        };
    }

    @Test
    void createFolder_getFolder_roundTrip() {
        Folder folder = storage.createFolder("MyFolder", TestData.PUBLIC, creator);
        assertNotNull(folder);
        assertEquals("MyFolder", folder.getName());
        assertSame(folder, storage.getFolder("MyFolder"));
    }

    @Test
    void getFolder_caseInsensitive() {
        storage.createFolder("MyFolder", TestData.PUBLIC, creator);
        assertNotNull(storage.getFolder("myfolder"));
        assertNotNull(storage.getFolder("MYFOLDER"));
        assertNotNull(storage.getFolder("MyFolder"));
    }

    @Test
    void folderExists_trueAfterCreate() {
        storage.createFolder("Alpha", TestData.ARCHIVE, creator);
        assertTrue(storage.folderExists("Alpha"));
        assertTrue(storage.folderExists("alpha"));
    }

    @Test
    void folderExists_falseWhenNotPresent() {
        assertFalse(storage.folderExists("NonExistent"));
    }

    @Test
    void getFolder_returnsNullWhenNotPresent() {
        assertNull(storage.getFolder("missing"));
    }

    @Test
    void getFolders_returnsAllCreated() {
        storage.createFolder("A", TestData.PUBLIC, creator);
        storage.createFolder("B", TestData.ARCHIVE, creator);
        assertEquals(2, storage.getFolders().size());
    }

    @Test
    void renamedFolder_isFoundAndRemovedUnderItsNewName() {
        Folder folder = storage.createFolder("Original", TestData.PUBLIC, creator);

        folder.setName("Renamed");

        assertSame(folder, storage.getFolder("renamed"));
        assertNull(storage.getFolder("Original"));
        storage.removeFolder("Renamed");
        assertTrue(storage.getFolders().isEmpty());
        assertEquals(List.of(folder), deletedFromStorage);
    }

    @Test
    void removeFolder_removesFromRegistry() {
        storage.createFolder("ToDelete", TestData.PUBLIC, creator);
        assertTrue(storage.folderExists("ToDelete"));

        storage.removeFolder("ToDelete");

        assertFalse(storage.folderExists("ToDelete"));
        assertNull(storage.getFolder("ToDelete"));
    }

    @Test
    void removeFolder_cascadesToSubfolders() {
        Folder parent = storage.createFolder("Parent", TestData.PUBLIC, creator);
        Folder child = storage.createFolder("Child", TestData.PUBLIC, creator);
        child.setParent(parent);

        storage.removeFolder(parent);

        assertFalse(storage.folderExists("Parent"));
        assertFalse(storage.folderExists("Child"));
    }

    @Test
    void removeFolder_deletesFolderAndSubfoldersFromStorage() {
        Folder parent = storage.createFolder("Parent", TestData.PUBLIC, creator);
        Folder child = storage.createFolder("Child", TestData.PUBLIC, creator);
        child.setParent(parent);
        storage.createFolder("Unrelated", TestData.PUBLIC, creator);

        storage.removeFolder(parent);

        assertEquals(2, deletedFromStorage.size());
        assertTrue(deletedFromStorage.containsAll(List.of(parent, child)));
    }

    @Test
    void createFolder_firesFolderCreatedEvent() {
        Folder folder = storage.createFolder("Evented", TestData.PUBLIC, creator);

        assertEquals(1, firedEvents.size());
        assertInstanceOf(FolderCreatedEvent.class, firedEvents.getFirst());
        assertSame(folder, ((FolderCreatedEvent) firedEvents.getFirst()).getFolder());
    }

    @Test
    void removeFolder_firesFolderDeletedEvent() {
        Folder folder = storage.createFolder("Evented", TestData.PUBLIC, creator);
        firedEvents.clear();

        storage.removeFolder("Evented");

        assertEquals(1, firedEvents.size());
        assertInstanceOf(FolderDeletedEvent.class, firedEvents.getFirst());
        assertSame(folder, ((FolderDeletedEvent) firedEvents.getFirst()).getFolder());
    }
}
