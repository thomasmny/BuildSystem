package de.eintosti.buildsystem.util.config;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.Location;

/**
 * @author einTosti
 */
public class SpawnConfig extends ConfigurationFile {

    public SpawnConfig(BuildSystem plugin) {
        super(plugin, "spawn.yml");
    }

    public void saveSpawn(Location location) {
        if (location == null) return;
        if (location.getWorld() == null) return;
        getFile().set("spawn", location.getWorld().getName() + ":"
                + location.getX() + ":"
                + location.getY() + ":"
                + location.getZ() + ":"
                + location.getYaw() + ":"
                + location.getPitch());
        saveFile();
    }
}
