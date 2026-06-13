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

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import org.jspecify.annotations.NullMarked;

/**
 * Root entry point of the BuildSystem API.
 *
 * <p>Through this interface you access the two primary services BuildSystem exposes:
 *
 * <ul>
 *   <li>{@link WorldService} — enumerate, create, load, unload and query worlds.
 *   <li>{@link PlayerService} — look up per-player settings and identity.
 * </ul>
 *
 * <h2>Obtaining an instance</h2>
 *
 * <p>The canonical way is via Bukkit's {@link org.bukkit.plugin.ServicesManager}:
 *
 * <pre>{@code
 * BuildSystem api = getServer().getServicesManager()
 *         .getRegistration(BuildSystem.class)
 *         .getProvider();
 * }</pre>
 *
 * <p>Or use the convenience shorthand {@link BuildSystemProvider#get()}.
 *
 * <p>The instance is registered during the BuildSystem plugin's {@code onEnable} and unregistered during
 * {@code onDisable}. Calling the provider before enable (e.g. during your plugin's {@code onLoad}) will throw
 * {@link IllegalStateException}.
 *
 * @since 3.0.0
 */
@NullMarked
public interface BuildSystem {

    /**
     * Returns the service managing all {@link BuildWorld} instances — world creation, lookup by name, folder
     * organisation and access to per-world storage.
     *
     * @return The world service, never {@code null}
     */
    WorldService getWorldService();

    /**
     * Returns the service managing all {@link BuildPlayer} instances — player lookup, settings access and session-state
     * tracking.
     *
     * @return The player service, never {@code null}
     */
    PlayerService getPlayerService();
}
