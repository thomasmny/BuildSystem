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
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.SetupConfig;
import de.eintosti.buildsystem.world.data.WorldStatus;
import de.eintosti.buildsystem.world.data.WorldType;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the default icons for different world types.
 */
public class WorldIcon {

    private final SetupConfig setupConfig;

    private final Map<WorldType, XMaterial> typeIcons;
    private final Map<WorldStatus, XMaterial> statusIcons;

    public WorldIcon(BuildSystem plugin) {
        this.setupConfig = new SetupConfig(plugin);

        this.typeIcons = loadTypeIcons();
        this.statusIcons = loadStatusIcons();
    }

    private Map<WorldType, XMaterial> loadTypeIcons() {
        return Optional
                .ofNullable(this.setupConfig.loadIcons("type", type -> WorldType.valueOf(type.toUpperCase(Locale.ROOT))))
                .orElseGet(() -> {
                    Map<WorldType, XMaterial> defaults = new EnumMap<>(WorldType.class);
                    defaults.put(WorldType.NORMAL, XMaterial.OAK_LOG);
                    defaults.put(WorldType.FLAT, XMaterial.GRASS_BLOCK);
                    defaults.put(WorldType.NETHER, XMaterial.NETHERRACK);
                    defaults.put(WorldType.END, XMaterial.END_STONE);
                    defaults.put(WorldType.VOID, XMaterial.GLASS);
                    defaults.put(WorldType.CUSTOM, XMaterial.FILLED_MAP);
                    defaults.put(WorldType.TEMPLATE, XMaterial.FILLED_MAP);
                    defaults.put(WorldType.IMPORTED, XMaterial.FURNACE);
                    return defaults;
                });
    }

    private Map<WorldStatus, XMaterial> loadStatusIcons() {
        return Optional
                .ofNullable(this.setupConfig.loadIcons("status", status -> WorldStatus.valueOf(status.toUpperCase(Locale.ROOT))))
                .orElseGet(() -> {
                    Map<WorldStatus, XMaterial> defaults = new EnumMap<>(WorldStatus.class);
                    defaults.put(WorldStatus.NOT_STARTED, XMaterial.RED_DYE);
                    defaults.put(WorldStatus.IN_PROGRESS, XMaterial.ORANGE_DYE);
                    defaults.put(WorldStatus.ALMOST_FINISHED, XMaterial.LIME_DYE);
                    defaults.put(WorldStatus.FINISHED, XMaterial.GREEN_DYE);
                    defaults.put(WorldStatus.ARCHIVE, XMaterial.CYAN_DYE);
                    defaults.put(WorldStatus.HIDDEN, XMaterial.BONE_MEAL);
                    return defaults;
                });
    }

    /**
     * Gets the icon for a specific world type.
     *
     * @param type The world type
     * @return The material to use as an icon
     */
    public XMaterial getIcon(WorldType type) {
        return this.typeIcons.get(type);
    }

    /**
     * Gets the icon for a specific world status.
     *
     * @param status The world status
     * @return The material to use as an icon
     */
    public XMaterial getIcon(WorldStatus status) {
        return this.statusIcons.get(status);
    }

    /**
     * Sets a custom icon for a world type.
     *
     * @param type     The world type
     * @param material The material to use as an icon
     */
    public void setIcon(WorldType type, XMaterial material) {
        this.typeIcons.put(type, material);
        this.setupConfig.saveIcon("type", type, material);
    }

    /**
     * Sets a custom icon for a world status.
     *
     * @param status   The world status
     * @param material The material to use as an icon
     */
    public void setIcon(WorldStatus status, XMaterial material) {
        this.statusIcons.put(status, material);
        this.setupConfig.saveIcon("status", status, material);
    }
}