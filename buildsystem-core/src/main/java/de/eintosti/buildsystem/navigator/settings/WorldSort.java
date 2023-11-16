/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator.settings;

import de.eintosti.buildsystem.Messages;
import org.bukkit.entity.Player;

import java.util.List;

public enum WorldSort {
    NAME_A_TO_Z("world_sort_name_az"),
    NAME_Z_TO_A("world_sort_name_za"),
    PROJECT_A_TO_Z("world_sort_project_az"),
    PROJECT_Z_TO_A("world_sort_project_za"),
    STATUS_NOT_STARTED("world_sort_status_not_started"),
    STATUS_FINISHED("world_sort_status_finished"),
    NEWEST_FIRST("world_sort_date_newest"),
    OLDEST_FIRST("world_sort_date_oldest");

    private final String loreKey;

    WorldSort(String loreKey) {
        this.loreKey = loreKey;
    }

    public List<String> getItemLore(Player player) {
        return Messages.getStringList(loreKey, player);
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

    public WorldSort getNext() {
        switch (this) {
            default: // NAME_A_TO_Z
                return NAME_Z_TO_A;
            case NAME_Z_TO_A:
                return PROJECT_A_TO_Z;
            case PROJECT_A_TO_Z:
                return PROJECT_Z_TO_A;
            case PROJECT_Z_TO_A:
                return STATUS_NOT_STARTED;
            case STATUS_NOT_STARTED:
                return STATUS_FINISHED;
            case STATUS_FINISHED:
                return NEWEST_FIRST;
            case NEWEST_FIRST:
                return OLDEST_FIRST;
            case OLDEST_FIRST:
                return NAME_A_TO_Z;
        }
    }

    public WorldSort getPrevious() {
        switch (this) {
            default: // NAME_A_TO_Z
                return OLDEST_FIRST;
            case NAME_Z_TO_A:
                return NAME_A_TO_Z;
            case PROJECT_A_TO_Z:
                return NAME_Z_TO_A;
            case PROJECT_Z_TO_A:
                return PROJECT_A_TO_Z;
            case STATUS_NOT_STARTED:
                return PROJECT_Z_TO_A;
            case STATUS_FINISHED:
                return STATUS_NOT_STARTED;
            case NEWEST_FIRST:
                return STATUS_FINISHED;
            case OLDEST_FIRST:
                return NEWEST_FIRST;
        }
    }
}