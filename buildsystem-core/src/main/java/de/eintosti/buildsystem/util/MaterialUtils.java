/*
 * Copyright (c) 2018-2023, Thomas Meaney
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
package de.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MaterialUtils {

    private MaterialUtils() {}

    /**
     * Resolves a persisted material name to a {@link Material}, tolerating pre-flattening names. XSeries stays an
     * implementation detail of the plugin: names are parsed through {@link XMaterial} so old data keeps loading, but
     * callers receive a concrete {@link Material}.
     *
     * @param name The persisted material name
     * @return The resolved material, or {@code null} if the name is unknown or unsupported by the running server
     */
    public static @Nullable Material match(@Nullable String name) {
        if (name == null) {
            return null;
        }
        return XMaterial.matchXMaterial(name).map(XMaterial::get).orElse(null);
    }
}
