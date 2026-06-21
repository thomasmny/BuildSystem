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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Displayable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DisplayOrdering {

    // Each comparator floats a tier of displayables to the top, applied in order before the user-selected sort.
    // To add a future criterion (e.g. favorites), add one floatToTop(...) entry here.
    private static final List<Comparator<Displayable>> PRIORITIES = List.of(floatToTop(DisplayOrdering::isPinned));

    private DisplayOrdering() {}

    /**
     * Wraps a user-selected sort so priority worlds (e.g. pinned) float to the top, then the given sort applies
     * within each tier.
     */
    public static Comparator<Displayable> withPriorities(Comparator<Displayable> sort) {
        return PRIORITIES.stream()
                .reduce(Comparator::thenComparing)
                .map(priority -> priority.thenComparing(sort))
                .orElse(sort);
    }

    private static Comparator<Displayable> floatToTop(Predicate<Displayable> flag) {
        return Comparator.comparing((Displayable displayable) -> flag.test(displayable))
                .reversed();
    }

    private static boolean isPinned(Displayable displayable) {
        return displayable instanceof BuildWorld world && world.getData().get(WorldDataKey.PINNED);
    }
}
