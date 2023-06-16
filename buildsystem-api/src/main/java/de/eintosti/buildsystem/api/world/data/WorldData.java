/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.Difficulty;

public interface WorldData {

    /**
     * Gets the {@link BuildWorld}'s custom spawn in the format {@code x;y;z;yaw;pitch}.
     *
     * @return The custom spawn location as a string
     */
    Type<String> customSpawn();

    /**
     * Gets the permission needed to enter the {@link BuildWorld}.
     *
     * @return The permission
     */
    Type<String> permission();

    /**
     * Gets the project description of the {@link BuildWorld}.
     *
     * @return The project description
     */
    Type<String> project();

    /**
     * Gets the difficulty of the {@link BuildWorld}.
     *
     * @return The bukkit world's difficulty
     */
    Type<Difficulty> difficulty();

    /**
     * Gets the material used to display the {@link BuildWorld} in the navigator.
     *
     * @return The material
     */
    Type<XMaterial> material();

    /**
     * Gets the current status of the {@link BuildWorld}.
     *
     * @return The current status
     */
    Type<WorldStatus> status();

    /**
     * Gets whether blocks can be broken in the {@link BuildWorld}.
     *
     * @return {@code true} if allowed, otherwise {@code false}
     */
    Type<Boolean> blockBreaking();

    /**
     * @return
     */
    Type<Boolean> blockInteractions();

    /**
     * Gets whether blocks can be placed in the {@link BuildWorld}.
     *
     * @return {@code true} if allowed, otherwise {@code false}
     */
    Type<Boolean> blockPlacement();

    /**
     * Gets whether the "builders feature" is enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> buildersEnabled();

    /**
     * Gets whether explosions are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> explosions();

    /**
     * Gets whether entities have an AI in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> mobAi();

    /**
     * Gets whether physics is applied to blocks in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> physics();

    /**
     * Gets whether the visibility of the {@link BuildWorld} is set to private.
     *
     * @return {@code true} if private, otherwise {@code false}
     */
    Type<Boolean> privateWorld();

    /**
     * Gets the timestamp of the last time the {@link BuildWorld} was last edited.
     *
     * @return The difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    Type<Long> lastEdited();

    /**
     * Gets the timestamp of the last time the {@link BuildWorld} was last loaded.
     *
     * @return The difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    Type<Long> lastLoaded();

    /**
     * Gets the timestamp of the last time the {@link BuildWorld} was last unloaded.
     *
     * @return The difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     */
    Type<Long> lastUnloaded();

    interface Type<T> {

        /**
         * Gets the current value.
         *
         * @return The current value
         */
        T get();

        /**
         * Sets the current value.
         *
         * @param value The value to set to
         */
        void set(T value);
    }
}