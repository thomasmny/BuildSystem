/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.PlayerManager;
import de.eintosti.buildsystem.api.world.WorldManager;

public class BuildSystemApi implements BuildSystem {

    private final BuildSystemPlugin plugin;

    public BuildSystemApi(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public WorldManager getWorldManager() {
        return plugin.getWorldManager();
    }

    @Override
    public PlayerManager getPlayerManager() {
        return plugin.getPlayerManager();
    }

    public void register() {
        BuildSystemProvider.register(this);
    }

    public void unregister() {
        BuildSystemProvider.unregister();
    }
}