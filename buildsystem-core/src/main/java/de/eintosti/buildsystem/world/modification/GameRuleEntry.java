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
package de.eintosti.buildsystem.world.modification;

import org.bukkit.GameRule;

/**
 * Represents a game rule entry consisting of a {@link GameRule} and its value.
 *
 * @param rule  The game rule
 * @param value The value of the game rule
 * @param <T>   The type of the game rule value; must be either {@link Boolean} or {@link Integer}
 */
public record GameRuleEntry<T>(GameRule<T> rule, T value) {

}

