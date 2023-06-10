/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.internal;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.version.customblocks.CustomBlocks;
import de.eintosti.buildsystem.version.gamerules.GameRules;
import de.eintosti.buildsystem.version.v1_12_R1.CustomBlocks_1_12_R1;
import de.eintosti.buildsystem.version.v1_12_R1.GameRules_1_12_R1;
import de.eintosti.buildsystem.version.v1_13_R1.CustomBlocks_1_13_R1;
import de.eintosti.buildsystem.version.v1_13_R1.GameRules_1_13_R1;
import de.eintosti.buildsystem.version.v1_14_R1.CustomBlocks_1_14_R1;
import de.eintosti.buildsystem.version.v1_17_R1.CustomBlocks_1_17_R1;
import de.eintosti.buildsystem.version.v1_20_R1.CustomBlocks_1_20_R1;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.List;

public enum ServerVersion {
    v1_8_R1(-1, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_8_R2(-1, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_8_R3(-1, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_9_R1(169, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_9_R2(184, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_10_R1(512, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_11_R1(922, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_12_R1(1343, CustomBlocks_1_12_R1.class, GameRules_1_12_R1.class),
    v1_13_R1(1519, CustomBlocks_1_13_R1.class, GameRules_1_13_R1.class),
    v1_13_R2(1631, CustomBlocks_1_13_R1.class, GameRules_1_13_R1.class),
    v1_14_R1(1976, CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_15_R1(2230, CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_16_R1(2567, CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_16_R2(2580, CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_16_R3(2586, CustomBlocks_1_14_R1.class, GameRules_1_13_R1.class),
    v1_17_R1(2730, CustomBlocks_1_17_R1.class, GameRules_1_13_R1.class),
    v1_18_R1(2865, CustomBlocks_1_17_R1.class, GameRules_1_13_R1.class),
    v1_18_R2(2975, CustomBlocks_1_17_R1.class, GameRules_1_13_R1.class),
    v1_19_R1(3120, CustomBlocks_1_17_R1.class, GameRules_1_13_R1.class),
    v1_19_R2(3218, CustomBlocks_1_17_R1.class, GameRules_1_13_R1.class),
    v1_19_R3(3337, CustomBlocks_1_17_R1.class, GameRules_1_13_R1.class),
    v1_20_R1(3463, CustomBlocks_1_20_R1.class, GameRules_1_13_R1.class),
    UNKNOWN;

    private final int dataVersion;
    private final Class<? extends CustomBlocks> customBlocks;
    private final Class<? extends GameRules> gameRules;

    private final BuildSystem plugin = JavaPlugin.getPlugin(BuildSystem.class);

    ServerVersion(int dataVersion, Class<? extends CustomBlocks> customBlocks, Class<? extends GameRules> gameRules) {
        this.dataVersion = dataVersion;
        this.customBlocks = customBlocks;
        this.gameRules = gameRules;
    }

    ServerVersion() {
        this.dataVersion = -1;
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

    /**
     * Gets the server's data version.
     * <p>
     * "The data version is a positive integer used in a world saved data to denote a specific version, and determines
     * whether the player should be warned about opening that world due to client version incompatibilities."
     *
     * @return The server's data version
     * @see <a href="https://minecraft.fandom.com/wiki/Data_version">Data version</a>
     */
    public int getDataVersion() {
        return dataVersion;
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
                    Messages.getString("worldeditor_gamerules_title"),
                    Messages.getStringList("worldeditor_gamerules_boolean_enabled"),
                    Messages.getStringList("worldeditor_gamerules_boolean_disabled"),
                    Messages.getStringList("worldeditor_gamerules_unknown"),
                    Messages.getStringList("worldeditor_gamerules_integer")
            );
            return (GameRules) instance;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}