package de.eintosti.buildsystem.util.config;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.generator.ChunkGenerator;

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

    public void saveWorld(BuildWorld buildWorld) {
        getFile().set("worlds." + buildWorld.getName(), buildWorld.serialize());
        saveFile();
    }

    public void loadWorlds(WorldManager worldManager) {
        if (plugin.isUnloadWorlds()) {
            plugin.getLogger().log(Level.INFO, "*** Unload worlds is enabled ***");
            plugin.getLogger().log(Level.INFO, "*** Therefore worlds will not be loaded ***");
            return;
        }

        plugin.getLogger().log(Level.INFO, "*** All worlds will be loaded now ***");

        worldManager.getBuildWorlds().forEach(world -> {
            String worldName = world.getName();
            ChunkGenerator chunkGenerator = world.getChunkGenerator();
            worldManager.generateBukkitWorld(worldName, world.getType(), chunkGenerator);

            if (world.getMaterial() == XMaterial.PLAYER_HEAD) {
                plugin.getSkullCache().cacheSkull(worldName);
            }

            plugin.getLogger().log(Level.INFO, "✔ World loaded: " + worldName);
        });

        plugin.getLogger().log(Level.INFO, "*** All worlds have been loaded ***");
    }
}
