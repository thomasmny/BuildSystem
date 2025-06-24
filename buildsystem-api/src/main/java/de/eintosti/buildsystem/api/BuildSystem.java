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
package de.eintosti.buildsystem.api;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;

/**
 * The BuildSystem API.
 *
 * @since 3.0.0
 */
public interface BuildSystem {

    /**
     * Gets the {@link WorldService}, responsible for managing {@link BuildWorld} instances.
     *
     * @return The world manager
     */
    WorldService getWorldService();

    /**
     * Gets the {@link PlayerService}, responsible for managing {@link BuildPlayer} instances.
     *
     * @return The player manager
     */
    PlayerService getPlayerService();
}