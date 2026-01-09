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
     * Manages the {@link BuildWorld}'s unload state.
     * <p>
     * If world unloading is enabled in the config, the unload task is started.
     */
    void manageUnload();

    /**
     * Starts a delayed task to unload the world, if world unloading is enabled in the config.
     */
    void startUnloadTask();

    /**
     * Resets the world unload task.
     */
    void resetUnloadTask();

    /**
     * Attempt to unload the world.
     * <p>
     * If the world contains any players, is blacklisted for unloading or is the spawn world, the unload will be canceled.
     */
    void unload();

    /**
     * Forces the unloading of the world, bypassing any checks or grace periods.
     *
     * @param save Whether the world should be saved before unloading
     */
    void forceUnload(boolean save);
} 