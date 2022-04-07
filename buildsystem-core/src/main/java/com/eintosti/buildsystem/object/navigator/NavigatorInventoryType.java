/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.navigator;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * @author einTosti
 */
public enum NavigatorInventoryType {
    NAVIGATOR("§aWorld Navigator"),
    ARCHIVE("§6World Archive"),
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

    public String getArmorStandName() {
        return armorStandName;
    }
}
