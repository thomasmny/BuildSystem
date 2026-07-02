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
package de.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Material name resolution for persisted data. XSeries stays an implementation detail of the plugin: names are parsed
 * through {@link XMaterial} so pre-flattening names keep loading, but callers receive a concrete {@link Material}.
 */
@NullMarked
public final class Materials {

    private Materials() {}

    /**
     * Resolves a persisted material name to a {@link Material}, tolerating legacy names. Returns the fallback for
     * {@code null} or unknown names and for materials the running server does not support.
     *
     * @param name The persisted material name
     * @param fallback The material to fall back to
     * @return The resolved material, or the fallback
     */
    public static Material match(@Nullable String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        return XMaterial.matchXMaterial(name).map(XMaterial::get).orElse(fallback);
    }
}
