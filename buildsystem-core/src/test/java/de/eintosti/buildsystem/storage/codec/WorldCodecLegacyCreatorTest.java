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
package de.eintosti.buildsystem.storage.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.test.TestData;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The creator formats of 2.x releases before 2.27, which stored the creator's name and id under separate keys. A world
 * saved by one of them still loads with the right creator.
 */
class WorldCodecLegacyCreatorTest {

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final PlayerLookupService lookup = mock(PlayerLookupService.class);
    private final WorldCodec codec = new WorldCodec(TestData.worldContext(), lookup);

    private BuildWorld load(String creator, String creatorId) {
        YamlConfiguration section = new YamlConfiguration();
        section.set("type", "NORMAL");
        section.set("date", 1_500_000_000_000L);
        section.set("data.status", "FINISHED");
        section.set("data.material", "GRASS_BLOCK");
        section.set("creator", creator);
        if (creatorId != null) {
            section.set("creator-id", creatorId);
        }
        return codec.deserialize(UUID.randomUUID().toString(), section);
    }

    private static void assertCreator(BuildWorld world) {
        Builder creator = world.getBuilders().getCreator();
        assertEquals(ALICE, creator.getUniqueId());
        assertEquals("Alice", creator.getName());
    }

    @Test
    void creatorIdIsUsedWithoutALookup() {
        BuildWorld world = load("Alice", ALICE.toString());

        assertCreator(world);
        verifyNoInteractions(lookup);
    }

    @Test
    void unknownCreatorIdIsLookedUpOnceByName() {
        when(lookup.lookupUniqueIdBlocking("Alice")).thenReturn(ALICE);

        BuildWorld world = load("Alice", "null");

        assertCreator(world);
        verify(lookup, times(1)).lookupUniqueIdBlocking("Alice");
    }

    @Test
    void unknownCreatorIdThatNoAccountHasLeavesNoCreator() {
        when(lookup.lookupUniqueIdBlocking("Alice")).thenReturn(null);

        assertNull(load("Alice", "null").getBuilders().getCreator());
    }

    @Test
    void dashMeansNoCreator() {
        assertNull(load("-", "null").getBuilders().getCreator());
        assertNull(load("-", null).getBuilders().getCreator());
        verifyNoInteractions(lookup);
    }
}
