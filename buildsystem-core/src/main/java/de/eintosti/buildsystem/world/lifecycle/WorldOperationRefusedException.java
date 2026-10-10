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
package de.eintosti.buildsystem.world.lifecycle;

import de.eintosti.buildsystem.api.exception.WorldException;
import org.jspecify.annotations.NullMarked;

/**
 * An operation on a world that was not carried out, because the world is busy with another operation or could not be
 * unloaded. The world and its folder are left as they were.
 */
@NullMarked
public final class WorldOperationRefusedException extends WorldException {

    private final String messageKey;

    private WorldOperationRefusedException(String message, String messageKey) {
        super(message);
        this.messageKey = messageKey;
    }

    static WorldOperationRefusedException busy(String worldName) {
        return new WorldOperationRefusedException(
                "World '%s' is busy with another operation".formatted(worldName), "worlds_world_busy");
    }

    static WorldOperationRefusedException notUnloaded(String worldName) {
        return new WorldOperationRefusedException(
                "World '%s' could not be unloaded".formatted(worldName), "worlds_world_unload_failed");
    }

    /**
     * {@return the message key that tells a player why the operation was refused, with a {@code %world%} placeholder}
     */
    public String messageKey() {
        return messageKey;
    }
}
