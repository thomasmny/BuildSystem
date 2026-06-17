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
package de.eintosti.buildsystem.world.display;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.storage.yaml.YamlSetupStorage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * User-customizable default icons for the world types used throughout the plugin. Status icons are configured per
 * status through the status registry, so they are no longer held here.
 */
@NullMarked
public class CustomizableIcons {

    private final YamlSetupStorage setupStorage;

    private final Map<BuildWorldType, XMaterial> typeIcons;

    public CustomizableIcons(BuildSystemPlugin plugin) {
        this.setupStorage = new YamlSetupStorage(plugin);

        this.typeIcons = loadTypeIcons();
    }

    private Map<BuildWorldType, XMaterial> loadTypeIcons() {
        Map<BuildWorldType, XMaterial> typeIcons = new EnumMap<>(Map.ofEntries(
                Map.entry(BuildWorldType.NORMAL, XMaterial.OAK_LOG),
                Map.entry(BuildWorldType.FLAT, XMaterial.GRASS_BLOCK),
                Map.entry(BuildWorldType.NETHER, XMaterial.NETHERRACK),
                Map.entry(BuildWorldType.END, XMaterial.END_STONE),
                Map.entry(BuildWorldType.VOID, XMaterial.GLASS),
                Map.entry(BuildWorldType.CUSTOM, XMaterial.FILLED_MAP),
                Map.entry(BuildWorldType.TEMPLATE, XMaterial.FILLED_MAP),
                Map.entry(BuildWorldType.IMPORTED, XMaterial.FURNACE)));

        Map<BuildWorldType, XMaterial> loadedIcons = this.setupStorage.loadIcons(
                IconType.TYPE, type -> BuildWorldType.valueOf(type.toUpperCase(Locale.ROOT)));
        if (loadedIcons != null) {
            typeIcons.putAll(loadedIcons);
        }

        setupStorage.saveIcons(IconType.TYPE, typeIcons);
        return typeIcons;
    }

    /**
     * Gets the icon for a specific {@link BuildWorldType}.
     *
     * @param type The world type
     * @return The material to use as an icon
     */
    public XMaterial getIcon(BuildWorldType type) {
        return this.typeIcons.get(type);
    }

    /**
     * Sets a custom icon for a {@link BuildWorldType}.
     *
     * @param type The world type
     * @param material The material to use as an icon
     */
    public void setIcon(BuildWorldType type, XMaterial material) {
        this.typeIcons.put(type, material);
        this.setupStorage.saveIcon(IconType.TYPE, type, material);
    }

    public enum IconType {
        TYPE;

        public String getKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
