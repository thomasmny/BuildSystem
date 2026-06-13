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
package de.eintosti.buildsystem.world.display;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class DisplayOrderingTest {

    @SuppressWarnings("unchecked")
    private static BuildWorld world(String name, boolean pinned) {
        BuildWorld world = mock(BuildWorld.class);
        WorldData data = mock(WorldData.class);
        Type<Boolean> pinnedType = mock(Type.class);
        when(world.getName()).thenReturn(name);
        when(world.getData()).thenReturn(data);
        when(data.pinned()).thenReturn(pinnedType);
        when(pinnedType.get()).thenReturn(pinned);
        return world;
    }

    private static Folder folder(String name) {
        Folder folder = mock(Folder.class);
        when(folder.getName()).thenReturn(name);
        return folder;
    }

    private static List<String> sortedNames(List<Displayable> input, Comparator<Displayable> comparator) {
        List<Displayable> copy = new ArrayList<>(input);
        copy.sort(comparator);
        return copy.stream().map(Displayable::getName).toList();
    }

    @Test
    void pinnedWorldsSortBeforeUnpinned_namesAscendingWithinGroup() {
        BuildWorld pinnedB = world("bravo", true);
        BuildWorld pinnedA = world("alpha", true);
        BuildWorld plainD = world("delta", false);
        BuildWorld plainC = world("charlie", false);

        List<String> result = sortedNames(
                List.of(plainD, pinnedB, plainC, pinnedA),
                DisplayOrdering.withPriorities(WorldSort.NAME_A_TO_Z.getComparator()));

        assertEquals(List.of("alpha", "bravo", "charlie", "delta"), result);
    }

    @Test
    void folderIsTreatedAsUnpinned_sortsAfterPinnedWorlds() {
        BuildWorld pinned = world("alpha", true);
        Folder folder = folder("aaa-folder");
        BuildWorld unpinned = world("zulu", false);

        List<String> result = sortedNames(
                List.of(folder, unpinned, pinned),
                DisplayOrdering.withPriorities(WorldSort.NAME_A_TO_Z.getComparator()));

        // The pinned world floats to the top even though the folder name sorts first alphabetically.
        assertEquals("alpha", result.get(0));
        assertEquals(List.of("alpha", "aaa-folder", "zulu"), result);
    }

    @Test
    void reversedSort_stillPlacesPinnedFirst_descendingWithinGroup() {
        BuildWorld pinnedA = world("alpha", true);
        BuildWorld pinnedB = world("bravo", true);
        BuildWorld plainC = world("charlie", false);
        BuildWorld plainD = world("delta", false);

        List<String> result = sortedNames(
                List.of(plainC, pinnedA, plainD, pinnedB),
                DisplayOrdering.withPriorities(WorldSort.NAME_Z_TO_A.getComparator()));

        // Pinned group first (descending), then unpinned group (descending).
        assertEquals(List.of("bravo", "alpha", "delta", "charlie"), result);
    }
}
