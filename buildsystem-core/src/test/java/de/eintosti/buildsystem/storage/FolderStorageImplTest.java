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

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class FolderStorageImplTest {

    private FolderStorageImpl storage;
    private Builder creator;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger(FolderStorageImplTest.class.getName());
        WorldStorage worldStorage = Mockito.mock(WorldStorage.class);
        creator = Builder.of(UUID.randomUUID(), "TestPlayer");

        storage = new FolderStorageImpl(logger, worldStorage) {
            @Override
            public CompletableFuture<Collection<Folder>> load() {
                return CompletableFuture.completedFuture(List.of());
            }

            @Override
            public CompletableFuture<Void> save(Folder folder) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> save(Collection<Folder> folders) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> delete(Folder folder) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> delete(String folderKey) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            protected Folder newFolder(
                    String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
                return new SimpleTestFolder(name, category, creator);
            }
        };
    }

    @Test
    void createFolder_getFolder_roundTrip() {
        Folder folder = storage.createFolder("MyFolder", NavigatorCategory.PUBLIC, creator);
        assertNotNull(folder);
        assertEquals("MyFolder", folder.getName());
        assertSame(folder, storage.getFolder("MyFolder"));
    }

    @Test
    void getFolder_caseInsensitive() {
        storage.createFolder("MyFolder", NavigatorCategory.PUBLIC, creator);
        assertNotNull(storage.getFolder("myfolder"));
        assertNotNull(storage.getFolder("MYFOLDER"));
        assertNotNull(storage.getFolder("MyFolder"));
    }

    @Test
    void folderExists_trueAfterCreate() {
        storage.createFolder("Alpha", NavigatorCategory.ARCHIVE, creator);
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
        storage.createFolder("A", NavigatorCategory.PUBLIC, creator);
        storage.createFolder("B", NavigatorCategory.ARCHIVE, creator);
        assertEquals(2, storage.getFolders().size());
    }

    // Minimal Folder implementation for tests
    @NullMarked
    private static final class SimpleTestFolder implements Folder {
        private final String name;
        private final NavigatorCategory category;
        private final Builder creator;

        @Nullable private Folder parent;

        SimpleTestFolder(String name, NavigatorCategory category, Builder creator) {
            this.name = name;
            this.category = category;
            this.creator = creator;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public NavigatorCategory getCategory() {
            return category;
        }

        @Override
        public Builder getCreator() {
            return creator;
        }

        @Override
        public long getCreation() {
            return 0;
        }

        @Override
        @Nullable public Folder getParent() {
            return parent;
        }

        @Override
        public void setParent(@Nullable Folder parent) {
            this.parent = parent;
        }

        @Override
        public boolean hasParent() {
            return parent != null;
        }

        @Override
        public List<UUID> getWorldUUIDs() {
            return List.of();
        }

        @Override
        public List<Folder> getSubFolders() {
            return List.of();
        }

        @Override
        public int getWorldCount() {
            return 0;
        }

        @Override
        public boolean containsWorld(de.eintosti.buildsystem.api.world.BuildWorld w) {
            return false;
        }

        @Override
        public boolean containsWorld(UUID uuid) {
            return false;
        }

        @Override
        public void addWorld(de.eintosti.buildsystem.api.world.BuildWorld w) {}

        @Override
        public void removeWorld(de.eintosti.buildsystem.api.world.BuildWorld w) {}

        @Override
        public void removeWorld(UUID uuid) {}

        @Override
        public boolean canView(org.bukkit.entity.Player player) {
            return true;
        }

        @Override
        public String getPermission() {
            return "-";
        }

        @Override
        public void setPermission(String p) {}

        @Override
        public String getProject() {
            return "-";
        }

        @Override
        public void setProject(String p) {}

        @Override
        public com.cryptomorin.xseries.XMaterial getIcon() {
            return com.cryptomorin.xseries.XMaterial.CHEST;
        }

        @Override
        public void setIcon(com.cryptomorin.xseries.XMaterial m) {}

        @Override
        public String getDisplayName(org.bukkit.entity.Player player) {
            return name;
        }

        @Override
        public List<String> getLore(org.bukkit.entity.Player player) {
            return List.of();
        }
    }
}
