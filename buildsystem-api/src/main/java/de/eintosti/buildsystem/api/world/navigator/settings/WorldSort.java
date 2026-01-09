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
package de.eintosti.buildsystem.api.world.navigator.settings;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import java.util.Comparator;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the sorting options for worlds in the navigator.
 *
 * @since 3.0.0
 */
@NullMarked
public enum WorldSort {

    /**
     * Sort worlds by name in ascending order.
     */
    NAME_A_TO_Z(Comparator.comparing(WorldSort::getNameSortKey)),

    /**
     * Sort worlds by name in descending order.
     */
    NAME_Z_TO_A(NAME_A_TO_Z.getComparator().reversed()),

    /**
     * Sort worlds by project in ascending order.
     */
    PROJECT_A_TO_Z(Comparator.comparing(WorldSort::getProjectSortKey)),

    /**
     * Sort worlds by project in descending order.
     */
    PROJECT_Z_TO_A(PROJECT_A_TO_Z.getComparator().reversed()),

    /**
     * Sort worlds by status ({@link BuildWorldStatus#NOT_STARTED} -> {@link BuildWorldStatus#FINISHED}).
     */
    STATUS_NOT_STARTED(Comparator.comparingInt(WorldSort::getStatusSortKey)),

    /**
     * Sort worlds by status ({@link BuildWorldStatus#FINISHED} -> {@link BuildWorldStatus#NOT_STARTED}).
     */
    STATUS_FINISHED(STATUS_NOT_STARTED.getComparator().reversed()),

    /**
     * Sort worlds by creation date in ascending order (oldest first).
     */
    OLDEST_FIRST(Comparator.comparingLong(Displayable::getCreation)),

    /**
     * Sort worlds by creation date in descending order (newest first).
     */
    NEWEST_FIRST(OLDEST_FIRST.getComparator().reversed());

    private final Comparator<Displayable> comparator;

    WorldSort(Comparator<Displayable> comparator) {
        this.comparator = comparator;
    }

    /**
     * Retrieves the name of a {@link Displayable} in lowercase for sorting purposes.
     *
     * @param displayable The {@link Displayable} item (e.g., {@link BuildWorld} or {@link Folder})
     * @return The lowercase name of the displayable
     */
    private static String getNameSortKey(Displayable displayable) {
        return displayable.getName().toLowerCase(Locale.ROOT);
    }

    /**
     * Retrieves the project name of a {@link Displayable} in lowercase for sorting purposes. If the displayable is a {@link BuildWorld}, its project name is returned. If it is a
     * {@link Folder}, its project is returned.
     *
     * @param displayable The {@link Displayable} item (e.g., {@link BuildWorld} or {@link Folder})
     * @return The lowercase project name, or an empty string if not applicable
     */
    private static String getProjectSortKey(Displayable displayable) {
        return switch (displayable) {
            case BuildWorld world -> world.getData().project().get().toLowerCase(Locale.ROOT);
            case Folder folder -> folder.getProject().toLowerCase(Locale.ROOT);
            default -> "";
        };
    }

    /**
     * Retrieves the status stage of a {@link Displayable} for sorting purposes. If the displayable is a {@link BuildWorld}, its status stage is returned. Otherwise,
     * {@link BuildWorldStatus#FINISHED} stage is returned.
     *
     * @param displayable The {@link Displayable} item (e.g., {@link BuildWorld} or {@link Folder})
     * @return The status stage integer
     */
    private static int getStatusSortKey(Displayable displayable) {
        if (displayable instanceof BuildWorld buildWorld) {
            return buildWorld.getData().status().get().getStage();
        }
        return BuildWorldStatus.FINISHED.getStage();
    }

    /**
     * Matches a string to a {@link WorldSort} enum constant.
     *
     * @param type The string to match
     * @return The matched {@link WorldSort} constant, or {@link WorldSort#NAME_A_TO_Z} if no match is found
     */
    public static WorldSort matchWorldSort(@Nullable String type) {
        if (type == null) {
            return NAME_A_TO_Z;
        }

        for (WorldSort value : values()) {
            if (value.toString().equalsIgnoreCase(type)) {
                return value;
            }
        }

        return NAME_A_TO_Z;
    }

    /**
     * Gets the pre-configured comparator for this sort order.
     *
     * @return The comparator used to sort {@link Displayable} items
     */
    public Comparator<Displayable> getComparator() {
        return this.comparator;
    }
}