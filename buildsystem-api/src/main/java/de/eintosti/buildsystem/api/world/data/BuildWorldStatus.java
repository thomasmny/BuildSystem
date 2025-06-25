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
package de.eintosti.buildsystem.api.world.data;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.Locale;

/**
 * Represents the various building statuses a {@link BuildWorld} can have. These statuses indicate the progression and accessibility of a world.
 *
 * @since 3.0.0
 */
public enum BuildWorldStatus {

    /**
     * Represents a {@link BuildWorld} that has not yet been started or modified. This is typically the initial state for newly created worlds.
     */
    NOT_STARTED(1),

    /**
     * Represents a {@link BuildWorld} that is currently under construction. This status is automatically assigned when a block is placed or broken in the world.
     */
    IN_PROGRESS(2),

    /**
     * Represents a {@link BuildWorld} that is nearing completion.
     */
    ALMOST_FINISHED(3),

    /**
     * Represents a {@link BuildWorld} whose building phase has been completed.
     */
    FINISHED(4),

    /**
     * Represents an older {@link BuildWorld} that has been completed and is now archived. Blocks typically cannot be placed or broken in archived worlds.
     */
    ARCHIVE(5),

    /**
     * Represents a {@link BuildWorld} that is hidden from public view in the navigator.
     */
    HIDDEN(6);

    private final int stage;

    BuildWorldStatus(int stage) {
        this.stage = stage;
    }

    /**
     * Gets the permission required to change a world to this status.
     *
     * @return The permission string (e.g., "buildsystem.setstatus.notstarted")
     */
    public String getPermission() {
        return "buildsystem.setstatus." + name().toLowerCase(Locale.ROOT).replace("_", "");
    }

    /**
     * Gets the development stage of the {@link BuildWorld}. A higher numerical value indicates a further developed or completed world.
     *
     * @return The integer representing the stage of development
     */
    public int getStage() {
        return stage;
    }
}