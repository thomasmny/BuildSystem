package de.eintosti.buildsystem.util.config;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class WorldConfig extends ConfigurationFile {
    private final BuildSystem plugin;

    public WorldConfig(BuildSystem plugin) {
        super(plugin, "worlds.yml");
        this.plugin = plugin;
    }

    public void saveWorld(World world) {
        getFile().set("worlds." + world.getName(), world.serialize());
        saveFile();
    }

    public void loadWorlds(WorldManager worldManager) {
        if (plugin.isUnloadWorlds()) {
            plugin.getLogger().log(Level.INFO, "*** Unload worlds is enabled ***");
            plugin.getLogger().log(Level.INFO, "*** Therefore worlds will not be loaded ***");
            return;
        }

        plugin.getLogger().log(Level.INFO, "*** All worlds will be loaded now ***");
        worldManager.getWorlds().forEach(world -> {
            worldManager.generateBukkitWorld(world.getName(), world.getType());
            plugin.getLogger().log(Level.INFO, "✔ World loaded: " + world.getName());
        });
        plugin.getLogger().log(Level.INFO, "*** All worlds have been loaded ***");
    }
}
