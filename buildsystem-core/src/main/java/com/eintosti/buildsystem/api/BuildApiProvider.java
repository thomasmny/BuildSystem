/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.api.settings.Settings;
import com.eintosti.buildsystem.api.world.BuildWorld;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.manager.WorldManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class BuildApiProvider implements BuildAPI {

    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    public BuildApiProvider(BuildSystem plugin) {
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();
    }

    @Override
    public List<BuildWorld> getBuildWorlds() {
        return new ArrayList<>(worldManager.getBuildWorlds());
    }

    @Override
    public BuildWorld getBuildWorld(String name) {
        return worldManager.getBuildWorld(name);
    }

    @Override
    public Settings getSettings(Player player) {
        return settingsManager.getSettings(player);
    }
}
