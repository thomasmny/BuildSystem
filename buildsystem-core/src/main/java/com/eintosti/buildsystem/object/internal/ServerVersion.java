/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.internal;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.version.CustomBlocks;
import com.eintosti.buildsystem.version.GameRules;
import com.eintosti.buildsystem.version.v1_12_R1.CustomBlocks_1_12_R1;
import com.eintosti.buildsystem.version.v1_12_R1.GameRules_1_12_R1;
import com.eintosti.buildsystem.version.v1_13_R1.CustomBlocks_1_13_R1;
import com.eintosti.buildsystem.version.v1_13_R1.GameRules_1_13_R1;
import com.eintosti.buildsystem.version.v1_14_R1.CustomBlocks_1_14_R1;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * @author einTosti
 */
public enum ServerVersion {

    v1_8_R1(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_8_R2(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_8_R3(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_9_R1(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_9_R2(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_10_R1(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_11_R1(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_12_R1(CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_13_R1(CustomBlocks_1_13_R1.class, GameRules_1_13_R1.class),
    v1_13_R2(CustomBlocks_1_13_R1.class, GameRules_1_13_R1.class),
    v1_14_R1(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_15_R1(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_16_R1(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_16_R2(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_16_R3(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_17_R1(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_18_R1(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_18_R2(CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    UNKNOWN;

    private final Class<? extends CustomBlocks> customBlocks;
    private final Class<? extends GameRules> gameRules;

    private final BuildSystem plugin = JavaPlugin.getPlugin(BuildSystem.class);

    ServerVersion(Class<? extends CustomBlocks> customBlocks, Class<? extends GameRules> gameRules) {
        this.customBlocks = customBlocks;
        this.gameRules = gameRules;
    }

    ServerVersion() {
        this.customBlocks = null;
        this.gameRules = null;
    }

    public static ServerVersion matchServerVersion(String version) {
        try {
            return ServerVersion.valueOf(version);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public CustomBlocks initCustomBlocks() {
        if (this == UNKNOWN || customBlocks == null) {
            return null;
        }

        try {
            Constructor<?> constructor = customBlocks.getConstructor(JavaPlugin.class);
            Object instance = constructor.newInstance(plugin);
            return (CustomBlocks) instance;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public GameRules initGameRules() {
        if (this == UNKNOWN || gameRules == null) {
            return null;
        }

        try {
            Constructor<?> constructor = gameRules.getConstructor(String.class, List.class, List.class, List.class, List.class);
            Object instance = constructor.newInstance(
                    plugin.getString("worldeditor_gamerules_title"),
                    plugin.getStringList("worldeditor_gamerules_boolean_enabled"),
                    plugin.getStringList("worldeditor_gamerules_boolean_disabled"),
                    plugin.getStringList("worldeditor_gamerules_unknown"),
                    plugin.getStringList("worldeditor_gamerules_integer")
            );
            return (GameRules) instance;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
