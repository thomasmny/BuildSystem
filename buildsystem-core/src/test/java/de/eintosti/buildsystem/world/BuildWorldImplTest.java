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
import static org.mockito.Mockito.*;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins world identity to the immutable {@link BuildWorldImpl#getUniqueId() UUID} rather than the mutable name, so a
 * renamed world stays findable in hash-based collections and two worlds are never conflated by sharing a name.
 */
@NullMarked
class BuildWorldImplTest {

    private BuildSystemPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getConfigService().current().world().unload().timeUntilUnload())
                .thenReturn("06:00:00");
    }

    private BuildWorldImpl world(String name, UUID uuid) {
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
                WorldContext.fromPlugin(plugin),
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
