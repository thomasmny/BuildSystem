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
package de.eintosti.buildsystem.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins world identity to the immutable {@link BuildWorldImpl#getUniqueId() UUID} rather than the mutable name, so a
 * renamed world stays findable in hash-based collections and two worlds are never conflated by sharing a name.
 */
@NullMarked
class BuildWorldImplTest {

    private WorldContext context;

    @BeforeEach
    void setUp() {
        context = TestData.worldContext();
    }

    private BuildWorldImpl world(String name, UUID uuid) {
        WorldDataImpl data = WorldDataSchema.create(name, TestData.NOT_STARTED);
        data.set(WorldDataKey.DIFFICULTY, Difficulty.NORMAL);
        data.set(WorldDataKey.MATERIAL, Material.GRASS_BLOCK);
        data.set(WorldDataKey.PERMISSION, "-");
        data.set(WorldDataKey.PROJECT, "-");
        data.set(WorldDataKey.VISIBILITY, Visibility.EVERYONE);
        return new BuildWorldImpl(
                context,
                uuid,
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
    void constructing_leavesTheUnloadTimerToTheCaller() {
        // Worlds are built on the async storage thread, so the constructor must not reach the Bukkit scheduler (which
        // this test has no server for) or decide the loaded state.
        when(context.configService().current().world().unload().enabled()).thenReturn(true);

        BuildWorldImpl world = world("Fresh", UUID.randomUUID());

        assertFalse(world.isLoaded());
    }

    @Test
    void identity_survivesRename() {
        BuildWorldImpl world = world("Original", UUID.randomUUID());
        Set<BuildWorldImpl> set = new HashSet<>();
        set.add(world);

        world.setName("Renamed");

        assertTrue(set.contains(world));
    }

    @Test
    void sameUuid_areEqual_regardlessOfName() {
        UUID uuid = UUID.randomUUID();
        assertEquals(world("A", uuid), world("B", uuid));
    }

    @Test
    void differentUuid_sameName_areNotEqual() {
        assertNotEquals(world("Same", UUID.randomUUID()), world("Same", UUID.randomUUID()));
    }
}
