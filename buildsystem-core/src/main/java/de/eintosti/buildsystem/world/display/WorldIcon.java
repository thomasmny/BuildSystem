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
import java.util.Optional;

/**
 * Manages the default icons for different world types.
 */
public class WorldIcon {

    private final SetupConfig setupConfig;

    private final Map<BuildWorldType, XMaterial> typeIcons;
    private final Map<BuildWorldStatus, XMaterial> statusIcons;

    public WorldIcon(BuildSystemPlugin plugin) {
        this.setupConfig = new SetupConfig(plugin);

        this.typeIcons = loadTypeIcons();
        this.statusIcons = loadStatusIcons();
    }

    private Map<BuildWorldType, XMaterial> loadTypeIcons() {
        return Optional
                .ofNullable(this.setupConfig.loadIcons("type", type -> BuildWorldType.valueOf(type.toUpperCase(Locale.ROOT))))
                .orElseGet(() -> {
                    Map<BuildWorldType, XMaterial> defaults = new EnumMap<>(BuildWorldType.class);
                    defaults.put(BuildWorldType.NORMAL, XMaterial.OAK_LOG);
                    defaults.put(BuildWorldType.FLAT, XMaterial.GRASS_BLOCK);
                    defaults.put(BuildWorldType.NETHER, XMaterial.NETHERRACK);
                    defaults.put(BuildWorldType.END, XMaterial.END_STONE);
                    defaults.put(BuildWorldType.VOID, XMaterial.GLASS);
                    defaults.put(BuildWorldType.CUSTOM, XMaterial.FILLED_MAP);
                    defaults.put(BuildWorldType.TEMPLATE, XMaterial.FILLED_MAP);
                    defaults.put(BuildWorldType.IMPORTED, XMaterial.FURNACE);
                    return defaults;
                });
    }

    private Map<BuildWorldStatus, XMaterial> loadStatusIcons() {
        return Optional
                .ofNullable(this.setupConfig.loadIcons("status", status -> BuildWorldStatus.valueOf(status.toUpperCase(Locale.ROOT))))
                .orElseGet(() -> {
                    Map<BuildWorldStatus, XMaterial> defaults = new EnumMap<>(BuildWorldStatus.class);
                    defaults.put(BuildWorldStatus.NOT_STARTED, XMaterial.RED_DYE);
                    defaults.put(BuildWorldStatus.IN_PROGRESS, XMaterial.ORANGE_DYE);
                    defaults.put(BuildWorldStatus.ALMOST_FINISHED, XMaterial.LIME_DYE);
                    defaults.put(BuildWorldStatus.FINISHED, XMaterial.GREEN_DYE);
                    defaults.put(BuildWorldStatus.ARCHIVE, XMaterial.CYAN_DYE);
                    defaults.put(BuildWorldStatus.HIDDEN, XMaterial.BONE_MEAL);
                    return defaults;
                });
    }

    /**
     * Gets the icon for a specific world type.
     *
     * @param type The world type
     * @return The material to use as an icon
     */
    public XMaterial getIcon(BuildWorldType type) {
        return this.typeIcons.get(type);
    }

    /**
     * Gets the icon for a specific world status.
     *
     * @param status The world status
     * @return The material to use as an icon
     */
    public XMaterial getIcon(BuildWorldStatus status) {
        return this.statusIcons.get(status);
    }

    /**
     * Sets a custom icon for a world type.
     *
     * @param type     The world type
     * @param material The material to use as an icon
     */
    public void setIcon(BuildWorldType type, XMaterial material) {
        this.typeIcons.put(type, material);
        this.setupConfig.saveIcon("type", type, material);
    }

    /**
     * Sets a custom icon for a world status.
     *
     * @param status   The world status
     * @param material The material to use as an icon
     */
    public void setIcon(BuildWorldStatus status, XMaterial material) {
        this.statusIcons.put(status, material);
        this.setupConfig.saveIcon("status", status, material);
    }
}