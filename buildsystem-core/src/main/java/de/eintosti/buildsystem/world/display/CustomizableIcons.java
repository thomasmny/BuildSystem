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
package de.eintosti.buildsystem.world.display;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.SetupConfig;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * User-customizable icons for different areas of the plugin.
 */
public class CustomizableIcons {

    private final SetupConfig setupConfig;

    private final Map<BuildWorldType, XMaterial> typeIcons;
    private final Map<BuildWorldStatus, XMaterial> statusIcons;

    public CustomizableIcons(BuildSystemPlugin plugin) {
        this.setupConfig = new SetupConfig(plugin);

        this.typeIcons = loadTypeIcons();
        this.statusIcons = loadStatusIcons();
    }

    private Map<BuildWorldType, XMaterial> loadTypeIcons() {
        Map<BuildWorldType, XMaterial> typeIcons = new EnumMap<>(BuildWorldType.class);
        typeIcons.put(BuildWorldType.NORMAL, XMaterial.OAK_LOG);
        typeIcons.put(BuildWorldType.FLAT, XMaterial.GRASS_BLOCK);
        typeIcons.put(BuildWorldType.NETHER, XMaterial.NETHERRACK);
        typeIcons.put(BuildWorldType.END, XMaterial.END_STONE);
        typeIcons.put(BuildWorldType.VOID, XMaterial.GLASS);
        typeIcons.put(BuildWorldType.CUSTOM, XMaterial.FILLED_MAP);
        typeIcons.put(BuildWorldType.TEMPLATE, XMaterial.FILLED_MAP);
        typeIcons.put(BuildWorldType.IMPORTED, XMaterial.FURNACE);

        Map<BuildWorldType, XMaterial> loadedIcons = this.setupConfig.loadIcons(IconType.TYPE, type -> BuildWorldType.valueOf(type.toUpperCase(Locale.ROOT)));
        if (loadedIcons != null) {
            typeIcons.putAll(loadedIcons);
        }

        setupConfig.saveIcons(IconType.TYPE, typeIcons);
        return typeIcons;
    }

    private Map<BuildWorldStatus, XMaterial> loadStatusIcons() {
        Map<BuildWorldStatus, XMaterial> statusIcon = new EnumMap<>(BuildWorldStatus.class);
        statusIcon.put(BuildWorldStatus.NOT_STARTED, XMaterial.RED_DYE);
        statusIcon.put(BuildWorldStatus.IN_PROGRESS, XMaterial.ORANGE_DYE);
        statusIcon.put(BuildWorldStatus.ALMOST_FINISHED, XMaterial.LIME_DYE);
        statusIcon.put(BuildWorldStatus.FINISHED, XMaterial.GREEN_DYE);
        statusIcon.put(BuildWorldStatus.ARCHIVE, XMaterial.CYAN_DYE);
        statusIcon.put(BuildWorldStatus.HIDDEN, XMaterial.BONE_MEAL);

        Map<BuildWorldStatus, XMaterial> loadedIcons = this.setupConfig.loadIcons(IconType.STATUS, type -> BuildWorldStatus.valueOf(type.toUpperCase(Locale.ROOT)));
        if (loadedIcons != null) {
            statusIcon.putAll(loadedIcons);
        }

        setupConfig.saveIcons(IconType.STATUS, statusIcon);
        return statusIcon;
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
     * Gets the icon for a specific {@link BuildWorldStatus}.
     *
     * @param status The world status
     * @return The material to use as an icon
     */
    public XMaterial getIcon(BuildWorldStatus status) {
        return this.statusIcons.get(status);
    }

    /**
     * Sets a custom icon for a {@link BuildWorldType}.
     *
     * @param type     The world type
     * @param material The material to use as an icon
     */
    public void setIcon(BuildWorldType type, XMaterial material) {
        this.typeIcons.put(type, material);
        this.setupConfig.saveIcon(IconType.TYPE, type, material);
    }

    /**
     * Sets a custom icon for a {@link BuildWorldStatus}.
     *
     * @param status   The world status
     * @param material The material to use as an icon
     */
    public void setIcon(BuildWorldStatus status, XMaterial material) {
        this.statusIcons.put(status, material);
        this.setupConfig.saveIcon(IconType.STATUS, status, material);
    }

    public enum IconType {
        TYPE,
        STATUS;

        public String getKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}