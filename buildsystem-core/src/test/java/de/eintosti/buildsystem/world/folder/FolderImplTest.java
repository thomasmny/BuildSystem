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
package de.eintosti.buildsystem.world.folder;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the world&lt;-&gt;folder single-owner invariant: assigning from either side keeps both the world's back-reference
 * and the folder's UUID list consistent, and the handshake terminates without infinite recursion.
 */
class FolderImplTest {

    private WorldContext context;

    @BeforeEach
    void setUp() {
        context = TestData.worldContext();
    }

    private FolderImpl folder(String name) {
        return new FolderImpl(context, name, TestData.PUBLIC, null, Builder.of(UUID.randomUUID(), "Creator"));
    }

    private BuildWorldImpl world(String name) {
        WorldDataImpl data = new WorldDataBuilder(name)
                .withStatus(TestData.NOT_STARTED)
                .withDifficulty(Difficulty.NORMAL)
                .withMaterial(XMaterial.GRASS_BLOCK)
                .withPermission("-")
                .withProject("-")
                .withVisibility(Visibility.EVERYONE)
                .withPermissionOverrideEnabled(() -> false)
                .withProjectOverrideEnabled(() -> false)
                .build();
        return new BuildWorldImpl(
                context,
                UUID.randomUUID(),
                name,
                BuildWorldType.NORMAL,
                data,
                Builder.of(UUID.randomUUID(), "Creator"),
                List.of(),
                System.currentTimeMillis(),
                null,
                null);
    }

    @Test
    void addWorld_setsBackReferenceAndMembership() {
        FolderImpl folder = folder("Target");
        BuildWorldImpl world = world("world");

        folder.addWorld(world);

        assertSame(folder, world.getFolder());
        assertTrue(folder.containsWorld(world));
    }

    @Test
    void setFolder_addsToFolderMembership() {
        FolderImpl folder = folder("Target");
        BuildWorldImpl world = world("world");

        world.setFolder(folder);

        assertSame(folder, world.getFolder());
        assertTrue(folder.containsWorld(world));
    }

    @Test
    void setFolderNull_removesFromFolderMembership() {
        FolderImpl folder = folder("Target");
        BuildWorldImpl world = world("world");
        folder.addWorld(world);

        world.setFolder(null);

        assertNull(world.getFolder());
        assertFalse(folder.containsWorld(world));
    }

    @Test
    void move_betweenFolders_leavesOnlyNewFolderMembership() {
        FolderImpl source = folder("Source");
        FolderImpl destination = folder("Destination");
        BuildWorldImpl world = world("world");
        source.addWorld(world);

        world.setFolder(destination);

        assertSame(destination, world.getFolder());
        assertTrue(destination.containsWorld(world));
        assertFalse(source.containsWorld(world));
    }

    @Test
    void removeWorld_clearsBothSides() {
        FolderImpl folder = folder("Target");
        BuildWorldImpl world = world("world");
        folder.addWorld(world);

        folder.removeWorld(world);

        assertNull(world.getFolder());
        assertFalse(folder.containsWorld(world));
    }

    @Test
    void addWorld_isIdempotent() {
        FolderImpl folder = folder("Target");
        BuildWorldImpl world = world("world");

        folder.addWorld(world);
        folder.addWorld(world);

        assertEquals(1, folder.getWorldUUIDs().size());
        assertEquals(1, folder.getWorldCount());
    }

    @Test
    void setParent_registersThenDeregistersSubFolder() {
        FolderImpl parent = folder("Parent");
        FolderImpl child = folder("Child");

        child.setParent(parent);
        assertTrue(parent.getSubFolders().contains(child));

        child.setParent(null);
        assertTrue(parent.getSubFolders().isEmpty());
    }

    @Test
    void setParent_repeatedDoesNotDuplicate() {
        FolderImpl parent = folder("Parent");
        FolderImpl child = folder("Child");

        child.setParent(parent);
        child.setParent(parent);

        assertEquals(1, parent.getSubFolders().size());
    }

    @Test
    void constructor_defensivelyCopiesWorlds() {
        List<UUID> worlds = new ArrayList<>(List.of(UUID.randomUUID()));
        FolderImpl folder = new FolderImpl(
                context,
                UUID.randomUUID(),
                "Detached",
                0L,
                TestData.PUBLIC,
                null,
                Builder.of(UUID.randomUUID(), "Creator"),
                XMaterial.CHEST,
                "-",
                "-",
                worlds,
                new ArrayList<>());

        worlds.clear();

        assertEquals(1, folder.getWorldUUIDs().size());
    }

    @Test
    void identity_survivesRename() {
        FolderImpl folder = folder("Original");
        Set<FolderImpl> set = new HashSet<>();
        set.add(folder);

        folder.setName("Renamed");

        assertTrue(set.contains(folder));
    }

    @Test
    void distinctFolders_sameName_areNotEqual() {
        assertNotEquals(folder("Same"), folder("Same"));
    }
}
