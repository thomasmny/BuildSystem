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

import de.eintosti.buildsystem.api.exception.WorldDeletionException;
import org.jspecify.annotations.NullMarked;

/**
 * A deletion that was not started because the world is busy with another operation or could not be unloaded. The world
 * and its folder are left as they were.
 */
@NullMarked
public final class WorldOperationRefusedException extends WorldDeletionException {

    private final String messageKey;

    public WorldOperationRefusedException(String message, String messageKey) {
        super(message);
        this.messageKey = messageKey;
    }

    /**
     * {@return the message key that tells a player why the operation was refused}
     */
    public String messageKey() {
        return messageKey;
    }
}
