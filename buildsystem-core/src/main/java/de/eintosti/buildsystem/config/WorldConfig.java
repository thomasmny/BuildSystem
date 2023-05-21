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
import org.bukkit.World;

import java.util.Iterator;
import java.util.logging.Logger;

public class WorldConfig extends ConfigurationFile {

    private final BuildSystem plugin;

    public WorldConfig(BuildSystem plugin) {
        super(plugin, "worlds.yml");
        this.plugin = plugin;
    }

    public void saveWorld(BuildWorld buildWorld) {
        getFile().set("worlds." + buildWorld.getName(), buildWorld.serialize());
        saveFile();
    }

    public void loadWorlds(WorldManager worldManager) {
        Logger logger = plugin.getLogger();
        if (plugin.getConfigValues().isUnloadWorlds()) {
            logger.info("*** \"Unload worlds\" has been enabled in the config. Therefore worlds will not be pre-loaded ***");
            return;
        }

        logger.info("*** All worlds will be loaded now ***");
        Iterator<BuildWorld> iterator = worldManager.getBuildWorlds().iterator();
        while (iterator.hasNext()) {
            BuildWorld buildWorld = iterator.next();
            String worldName = buildWorld.getName();
            World world = new BuildWorldCreator(plugin, buildWorld).generateBukkitWorld();
            if (world == null) {
                iterator.remove();
                continue;
            }

            if (buildWorld.getData().material().get() == XMaterial.PLAYER_HEAD) {
                plugin.getSkullCache().cacheSkull(worldName);
            }

            logger.info("✔ World loaded: " + worldName);
        }
        logger.info("*** All worlds have been loaded ***");
    }
}