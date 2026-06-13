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

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.NullMarked;

/**
 * Static accessor for the {@link BuildSystem} API instance.
 *
 * <p>Use {@link #get()} to retrieve the active API once BuildSystem has enabled. This is a convenience shorthand around
 * Bukkit's {@link org.bukkit.plugin.ServicesManager}, where the BuildSystem plugin registers itself during
 * {@code onEnable}; resolving the service directly returns the same object.
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
        RegisteredServiceProvider<BuildSystem> registration =
                Bukkit.getServicesManager().getRegistration(BuildSystem.class);
        if (registration == null) {
            throw new IllegalStateException("BuildSystem has not loaded yet!");
        }
        return registration.getProvider();
    }
}
