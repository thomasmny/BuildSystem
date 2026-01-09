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
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Provides utilities for loading and managing {@link BuildWorld}s. This interface handles the process of making a world accessible on the server.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldLoader {

    /**
     * Loads the world associated with this loader for a specific player. This typically involves teleporting the player to the world after it's loaded.
     *
     * @param player The {@link Player} for whom the world should be loaded and who will be teleported into it
     */
    void loadForPlayer(Player player);

    /**
     * Loads the world associated with this loader without teleporting any specific player. This is useful for background world loading or server-side operations.
     */
    void load();
} 