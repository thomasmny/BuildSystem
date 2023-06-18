/*
 * Copyright (c) 2018-2023, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.config;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.world.BuildWorldManager;
import de.eintosti.buildsystem.world.CraftBuildWorld;
import de.eintosti.buildsystem.world.CraftBuildWorldCreator;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class WorldConfig extends ConfigurationFile {

    private final BuildSystemPlugin plugin;

    public WorldConfig(BuildSystemPlugin plugin) {
        super(plugin, "worlds.yml");
        this.plugin = plugin;
    }

    public void saveWorlds(Collection<CraftBuildWorld> buildWorlds) {
        buildWorlds.forEach(buildWorld -> getFile().set("worlds." + buildWorld.getName(), buildWorld.serialize()));
        saveFile();
    }

    public void loadWorlds(BuildWorldManager worldManager) {
        Logger logger = plugin.getLogger();
        if (plugin.getConfigValues().isUnloadWorlds()) {
            logger.info("*** \"Unload worlds\" has been enabled in the config. Therefore worlds will not be pre-loaded ***");
            return;
        }

        logger.info("*** All worlds will be loaded now ***");

        List<BuildWorld> notLoaded = new ArrayList<>();
        worldManager.getBuildWorlds().forEach(buildWorld -> {
            String worldName = buildWorld.getName();
            World world = new CraftBuildWorldCreator(plugin, buildWorld).generateBukkitWorld();
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