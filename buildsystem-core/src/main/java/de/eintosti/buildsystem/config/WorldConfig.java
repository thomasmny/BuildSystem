/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.config;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.BuildWorldCreator;
import de.eintosti.buildsystem.world.WorldManager;
import de.eintosti.buildsystem.world.data.WorldData;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class WorldConfig extends ConfigurationFile {

    private final BuildSystem plugin;

    public WorldConfig(BuildSystem plugin) {
        super(plugin, "worlds.yml");
        this.plugin = plugin;
    }

    public void saveWorlds(Collection<BuildWorld> buildWorlds) {
        buildWorlds.forEach(buildWorld -> getFile().set("worlds." + buildWorld.getName(), buildWorld.serialize()));
        saveFile();
    }

    public void loadWorlds(WorldManager worldManager) {
        Logger logger = plugin.getLogger();
        if (plugin.getConfigValues().isUnloadWorlds()) {
            logger.info("*** \"Unload worlds\" has been enabled in the config. Therefore worlds will not be pre-loaded ***");
            return;
        }

        logger.info("*** All worlds will be loaded now ***");

        List<BuildWorld> notLoaded = new ArrayList<>();
        worldManager.getBuildWorlds().forEach(buildWorld -> {
            String worldName = buildWorld.getName();
            World world = new BuildWorldCreator(plugin, buildWorld).generateBukkitWorld();
            if (world == null) {
                notLoaded.add(buildWorld);
                return;
            }

            WorldData worldData = buildWorld.getData();
            worldData.lastLoaded().set(System.currentTimeMillis());
            if (worldData.material().get() == XMaterial.PLAYER_HEAD) {
                plugin.getSkullCache().cacheSkull(worldName);
            }

            logger.info("✔ World loaded: " + worldName);
        });
        notLoaded.forEach(worldManager::removeBuildWorld);

        logger.info("*** All worlds have been loaded ***");
    }
}