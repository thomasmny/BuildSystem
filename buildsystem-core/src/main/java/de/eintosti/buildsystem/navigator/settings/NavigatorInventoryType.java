/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator.settings;

import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.Builder;
import de.eintosti.buildsystem.world.data.WorldData;
import de.eintosti.buildsystem.world.data.WorldStatus;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the different kinds of navigator types that can be opened.
 *
 * @author einTosti
 */
public enum NavigatorInventoryType {
    /**
     * The navigator inventory which contains all {@link BuildWorld}s that are still being built.
     */
    NAVIGATOR("§aWorld Navigator"),

    /**
     * The navigator inventory which contains all archived {@link BuildWorld}s.
     *
     * @see WorldStatus#ARCHIVE
     */
    ARCHIVE("§6World Archive"),

    /**
     * The navigator inventory which contains all private {@link BuildWorld}s that can only be modified by the world's creator
     * and all players who have been added as a {@link Builder}.
     *
     * @see WorldData#privateWorld()
     */
    PRIVATE("§bPrivate Worlds");

    private final String armorStandName;

    NavigatorInventoryType(String armorStandName) {
        this.armorStandName = armorStandName;
    }

    @Nullable
    public static NavigatorInventoryType matchInventoryType(Player player, String customName) {
        String typeName = customName.replace(player.getName() + " × ", "");

        for (NavigatorInventoryType navigatorInventoryType : values()) {
            if (navigatorInventoryType.getArmorStandName().equalsIgnoreCase(typeName)) {
                return navigatorInventoryType;
            }
        }

        return null;
    }

    /**
     * When opening the {@link NavigatorType#NEW} navigator, each armor stand has a custom name which is unique for every player
     * and each {@link NavigatorInventoryType}.<br>
     * Gets said name.
     *
     * @return The name which an armor stand uses to represent a navigator inventory type
     */
    public String getArmorStandName() {
        return armorStandName;
    }
}