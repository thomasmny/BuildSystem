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
package de.eintosti.buildsystem.api.world.util;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jspecify.annotations.NullMarked;

/**
 * Provides utilities for managing the unloading process of a {@link BuildWorld}. This interface handles tasks such as initiating and cancelling unload procedures.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldUnloader {

    /**
     * Manages the scheduled unloading of the world. This method is typically called periodically to check if the world can be safely unloaded based on activity or configured
     * delays.
     */
    void manageUnload();

    /**
     * Initiates a task that will attempt to unload the world after a certain period of inactivity. If an unload task is already running, it will be reset or prolonged.
     */
    void startUnloadTask();

    /**
     * Resets or cancels any currently active world unload task. This is useful when the world becomes active again and should not be unloaded prematurely.
     */
    void resetUnloadTask();

    /**
     * Unloads the world associated with this unloader. This method attempts a graceful unload, ensuring players are moved out and world data is saved.
     */
    void unload();

    /**
     * Forces the unloading of the world, bypassing any checks or grace periods. This is typically used for immediate cleanup or in critical situations.
     *
     * @param save If {@code true}, the world data will be saved before unloading; otherwise, changes will be discarded.
     */
    void forceUnload(boolean save);
} 