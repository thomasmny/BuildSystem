/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api;

import org.jetbrains.annotations.NotNull;

public class BuildSystemProvider {

    private static BuildSystem instance;

    @NotNull
    public static BuildSystem get() {
        BuildSystem instance = BuildSystemProvider.instance;
        if (instance == null) {
            throw new IllegalStateException("BuildSystem has not loaded yet!");
        }
        return instance;
    }

    static void set(BuildSystem impl) {
        BuildSystemProvider.instance = impl;
    }

    private BuildSystemProvider() {
        throw new AssertionError();
    }
}