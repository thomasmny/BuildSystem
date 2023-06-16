/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class BuildSystemProvider {

    private static BuildSystem instance = null;

    /**
     * Gets an instance of the {@link BuildSystem} API.
     *
     * @return An instance of the BuildSystem API
     * @throws IllegalStateException if the API is not loaded yet
     */
    @NotNull
    public static BuildSystem get() {
        BuildSystem instance = BuildSystemProvider.instance;
        if (instance == null) {
            throw new IllegalStateException("BuildSystem has not loaded yet!");
        }
        return instance;
    }

    @ApiStatus.Internal
    static void register(BuildSystem instance) {
        BuildSystemProvider.instance = instance;
    }

    @ApiStatus.Internal
    static void unregister() {
        BuildSystemProvider.instance = null;
    }

    @ApiStatus.Internal
    private BuildSystemProvider() {
        throw new AssertionError();
    }
}