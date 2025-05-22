/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.api.navigator.settings;

import org.jetbrains.annotations.ApiStatus.Internal;

public enum WorldSort {

    /**
     * Sort worlds by name in ascending order.
     */
    NAME_A_TO_Z("world_sort_name_az"),

    /**
     * Sort worlds by name in descending order.
     */
    NAME_Z_TO_A("world_sort_name_za"),

    /**
     * Sort worlds by project in ascending order.
     */
    PROJECT_A_TO_Z("world_sort_project_az"),

    /**
     * Sort worlds by project in descending order.
     */
    PROJECT_Z_TO_A("world_sort_project_za"),

    /**
     * Sort worlds by status ("Not Started" -> "Finished").
     */
    STATUS_NOT_STARTED("world_sort_status_not_started"),

    /**
     * Sort worlds by status ("Finished" -> "Not Started").
     */
    STATUS_FINISHED("world_sort_status_finished"),

    /**
     * Sort worlds by creation date in descending order (newest first).
     */
    NEWEST_FIRST("world_sort_date_newest"),

    /**
     * Sort worlds by creation date in ascending order (oldest first).
     */
    OLDEST_FIRST("world_sort_date_oldest");

    private final String messageKey;

    WorldSort(String messageKey) {
        this.messageKey = messageKey;
    }

    @Internal
    public String getMessageKey() {
        return messageKey;
    }

    public static WorldSort matchWorldSort(String type) {
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
}