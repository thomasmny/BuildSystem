/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world.data;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Difficulty;

public interface WorldData {

    Type<String> customSpawn();

    Type<String> permission();

    Type<String> project();

    Type<Difficulty> difficulty();

    Type<XMaterial> material();

    Type<WorldStatus> status();

    Type<Boolean> blockBreaking();

    Type<Boolean> blockInteractions();

    Type<Boolean> blockPlacement();

    Type<Boolean> buildersEnabled();

    Type<Boolean> explosions();

    Type<Boolean> mobAi();

    Type<Boolean> physics();

    Type<Boolean> privateWorld();

    Type<Long> lastEdited();

    Type<Long> lastLoaded();

    Type<Long> lastUnloaded();

    interface Type<T> {

        T get();

        void set(T value);
    }
}