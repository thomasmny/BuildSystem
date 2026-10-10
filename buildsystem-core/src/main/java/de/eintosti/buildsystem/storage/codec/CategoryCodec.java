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
package de.eintosti.buildsystem.storage.codec;

import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.util.MaterialUtils;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;

/** {@link Codec} for the navigator categories in {@code categories.yml}, keyed by category id. */
@NullMarked
public final class CategoryCodec implements Codec<NavigatorCategoryImpl> {

    @Override
    public String key(NavigatorCategoryImpl category) {
        return category.getId();
    }

    @Override
    public Map<String, Object> serialize(NavigatorCategoryImpl category) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("display-name", category.getDisplayName());
        serialized.put("color", category.getColor());
        serialized.put("icon", category.getIcon().name());
        Codec.putIfPresent(serialized, "icon-skull-texture", category.getIconSkullTexture());
        serialized.put(
                "visibilities",
                category.getVisibilities().stream().map(Visibility::name).toList());
        serialized.put("shown-in-navigator", category.isShown());
        serialized.put("navigator-slot", category.getSlot());
        serialized.put("built-in", category.isBuiltIn());
        serialized.put("statuses", category.getStatusIds());
        return serialized;
    }

    @Override
    public NavigatorCategoryImpl deserialize(String id, ConfigurationSection section) {
        EnumSet<Visibility> visibilities = EnumSet.noneOf(Visibility.class);
        for (String raw : section.getStringList("visibilities")) {
            try {
                visibilities.add(Visibility.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Skip unknown visibility names rather than failing the whole category.
            }
        }
        if (visibilities.isEmpty()) {
            visibilities.add(Visibility.EVERYONE);
        }

        Material icon = Objects.requireNonNullElse(MaterialUtils.match(section.getString("icon")), Material.OAK_SIGN);
        return NavigatorCategoryImpl.builder(id)
                .displayName(section.getString("display-name", id))
                .color(section.getString("color", "&7"))
                .icon(icon)
                .iconSkullTexture(section.getString("icon-skull-texture"))
                .visibilities(visibilities)
                .shownInNavigator(section.getBoolean("shown-in-navigator", true))
                .navigatorSlot(section.getInt("navigator-slot", 0))
                .builtIn(section.getBoolean("built-in", false))
                .statusIds(section.getStringList("statuses"))
                .build();
    }
}
