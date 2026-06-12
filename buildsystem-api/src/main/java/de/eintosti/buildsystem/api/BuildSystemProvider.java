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
package de.eintosti.buildsystem.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Static accessor for the {@link BuildSystem} API instance.
 *
 * <p>Use {@link #get()} to retrieve the active API once BuildSystem has enabled. If you prefer dependency-injection
 * style, retrieve the same instance from Bukkit's {@link org.bukkit.plugin.ServicesManager} instead — both paths return
 * the same object.
 *
 * <h2>Lifecycle</h2>
 *
 * <ul>
 *   <li>Available after BuildSystem's {@code onEnable} fires.
 *   <li>Unavailable (throws) before enable and after BuildSystem's {@code onDisable}.
 * </ul>
 *
 * @since 3.0.0
 */
@NullMarked
public class BuildSystemProvider {

    private static @Nullable BuildSystem instance = null;

    private BuildSystemProvider() {
        throw new AssertionError("This class is not meant to be instantiated");
    }

    /**
     * Returns the active {@link BuildSystem} API instance.
     *
     * @return The API instance
     * @throws IllegalStateException if BuildSystem has not finished enabling yet, or has already disabled
     */
    public static BuildSystem get() {
        BuildSystem instance = BuildSystemProvider.instance;
        if (instance == null) {
            throw new IllegalStateException("BuildSystem has not loaded yet!");
        }
        return instance;
    }

    /**
     * Binds the active API instance. Called internally by the BuildSystem plugin during {@code onEnable} — other
     * plugins must <strong>not</strong> call this method.
     *
     * @param instance The API instance to publish
     */
    public static void register(BuildSystem instance) {
        BuildSystemProvider.instance = instance;
    }

    /**
     * Clears the active API instance. Called internally by the BuildSystem plugin during {@code onDisable} — other
     * plugins must <strong>not</strong> call this method.
     */
    public static void unregister() {
        BuildSystemProvider.instance = null;
    }
}
