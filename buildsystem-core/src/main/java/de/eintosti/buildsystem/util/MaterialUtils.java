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

    /**
     * Maps a standing or hanging sign to the variant placed against a wall, for example {@code OAK_SIGN} to
     * {@code OAK_WALL_SIGN} and {@code OAK_HANGING_SIGN} to {@code OAK_WALL_HANGING_SIGN}.
     *
     * @param sign The sign item's material
     * @return The wall variant, or {@code null} if {@code sign} is not a standing or hanging sign
     */
    public static @Nullable Material wallVariant(Material sign) {
        String name = sign.name();
        if (!name.endsWith("_SIGN") || name.contains("_WALL_")) {
            return null;
        }
        String suffix = name.endsWith("_HANGING_SIGN") ? "_HANGING_SIGN" : "_SIGN";
        return Material.matchMaterial(name.substring(0, name.length() - suffix.length()) + "_WALL" + suffix);
    }
}
