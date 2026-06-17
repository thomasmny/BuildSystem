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
import static org.mockito.Mockito.*;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import java.util.List;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the world&lt;-&gt;folder single-owner invariant: assigning from either side keeps both the world's back-reference
 * and the folder's UUID list consistent, and the handshake terminates without infinite recursion.
 */
class FolderImplTest {

    private BuildSystemPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getConfigService().current().world().unload().timeUntilUnload())
                .thenReturn("06:00:00");
    }

    private FolderImpl folder(String name) {
        return new FolderImpl(plugin, name, TestData.PUBLIC, null, Builder.of(UUID.randomUUID(), "Creator"));
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
                plugin,
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
}
