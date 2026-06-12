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
package de.eintosti.buildsystem.i18n;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Applies external placeholder expansion to a message after BuildSystem's own substitution. Implementations live in {@code integration/} so that {@link Messages} never imports a
 * soft-dependency type; one is registered while the providing plugin is present and cleared when it is not.
 */
@NullMarked
@FunctionalInterface
public interface TextResolver {

    /**
     * Expands placeholders in the given text for the given player.
     *
     * @param player The player the text is rendered for
     * @param text   The text to expand
     * @return The expanded text
     */
    String resolve(Player player, String text);
}
