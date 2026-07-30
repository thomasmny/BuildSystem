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

import org.jspecify.annotations.NullMarked;

/**
 * The two flags that shape a {@link WorldServiceImpl#startWorldNameInput} prompt flow, grouped so call sites carry
 * named components instead of two adjacent, easily transposed booleans.
 *
 * @param privateWorld Whether the created world should be private
 * @param promptSeed Whether the flow should ask the player for a seed before building the world
 */
@NullMarked
public record WorldNameInputOptions(boolean privateWorld, boolean promptSeed) {}
