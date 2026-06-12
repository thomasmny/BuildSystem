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
package de.eintosti.buildsystem.api.player;

import de.eintosti.buildsystem.api.player.settings.Settings;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * A player managed by BuildSystem.
 *
 * <p>Identity ({@link #getUniqueId()}) is stable across sessions; {@linkplain #getSettings() settings} are mutable. Anything beyond identity and settings — cached gameplay state,
 * logout location, navigator UI state — is internal runtime state and lives outside this interface so the public surface stays focused.</p>
 *
 * @since 3.0.0
 */
@NullMarked
public interface BuildPlayer {

    /**
     * Returns the player's UUID. Equal to {@link Player#getUniqueId()} for the wrapped player.
     *
     * @return The player's UUID
     */
    UUID getUniqueId();

    /**
     * Returns the player's BuildSystem settings (display preferences, navigator type, scoreboard etc.).
     *
     * @return The player's settings
     */
    Settings getSettings();
}
