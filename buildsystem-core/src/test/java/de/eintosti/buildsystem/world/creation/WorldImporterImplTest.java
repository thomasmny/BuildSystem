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
package de.eintosti.buildsystem.world.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.event.world.BuildWorldPostCreateEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import java.io.File;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

@NullMarked
class WorldImporterImplTest {

    @TempDir
    File worldContainer;

    private final WorldContext context = TestData.worldContext();
    private ServerMock server;
    private WorldStorageImpl worldStorage;

    @BeforeEach
    void setUp() {
        // MockBukkit has no world container and its worlds have no folder; the import needs both.
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public File getWorldContainer() {
                return worldContainer;
            }

            @Override
            public @Nullable World createWorld(WorldCreator creator) {
                World created = super.createWorld(creator);
                if (created == null) {
                    return null;
                }
                World world = spy(created);
                doReturn(new File(worldContainer, creator.name())).when(world).getWorldFolder();
                return world;
            }
        });
        worldStorage = mock(WorldStorageImpl.class);
        PluginConfig.World.Defaults defaults =
                context.configService().current().world().defaults();
        when(defaults.permission().publicPermission()).thenReturn("-");
        when(defaults.difficulty()).thenReturn(Difficulty.PEACEFUL);
        when(context.customizableIcons().getIcon(any())).thenReturn(Material.GRASS_BLOCK);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void importWithPluginGenerator_registersAnImportedWorld() {
        BuildWorld imported = new WorldImporterImpl(context, worldStorage, "terra_world")
                .customGenerator(new CustomGeneratorImpl("Terra", "Terra", null))
                .build();

        assertNotNull(imported);
        assertEquals(BuildWorldType.IMPORTED, imported.getType());
        verify(worldStorage).addBuildWorld(imported);
        verify(worldStorage, never()).removeBuildWorld(any());
        assertEquals(1, postCreateEvents());
    }

    @Test
    void failedGeneration_leavesNoRegisteredWorld() {
        // Not a valid world key, so the server refuses to create it.
        BuildWorld imported = new WorldImporterImpl(context, worldStorage, "maps:not valid").build();

        assertNull(imported);
        verify(worldStorage).removeBuildWorld(any());
        assertEquals(0, postCreateEvents(), "a failed import is not a created world");
    }

    private long postCreateEvents() {
        return server.getPluginManager()
                .getFiredEvents()
                .filter(BuildWorldPostCreateEvent.class::isInstance)
                .count();
    }
}
