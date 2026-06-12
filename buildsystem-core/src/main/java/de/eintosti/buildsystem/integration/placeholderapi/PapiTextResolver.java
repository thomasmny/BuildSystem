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
package de.eintosti.buildsystem.integration.placeholderapi;

import de.eintosti.buildsystem.i18n.TextResolver;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** Resolves PlaceholderAPI placeholders. Registered on {@code Messages} only while PlaceholderAPI is present. */
@NullMarked
public final class PapiTextResolver implements TextResolver {

    @Override
    public String resolve(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
