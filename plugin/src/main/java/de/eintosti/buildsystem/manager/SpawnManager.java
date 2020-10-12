package de.eintosti.buildsystem.manager;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.config.SpawnConfig;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class SpawnManager {
    private final WorldManager worldManager;
    private final SpawnConfig spawnConfig;

    private String spawnName;
    private Location spawn;

    public SpawnManager(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        this.spawnConfig = new SpawnConfig(plugin);
    }

    public boolean teleport(Player player) {
        if (!spawnExists()) return false;

        World world = worldManager.getWorld(spawnName);
        if (world != null) {
            if (!world.isLoaded()) world.load(player);
        }

        player.setFallDistance(0);
        player.teleport(spawn);
        Titles.clearTitle(player);

        return true;
    }

    public boolean spawnExists() {
        return spawn != null;
    }

    public Location getSpawn() {
        return spawn;
    }

    public org.bukkit.World getSpawnWorld() {
        return spawn.getWorld();
    }

    public void set(Location location, String worldName) {
        this.spawn = location;
        this.spawnName = worldName;
    }

    public void remove() {
        this.spawn = null;
    }

    public void save() {
        spawnConfig.saveSpawn(spawn);
    }

    public void load() {
        FileConfiguration configuration = spawnConfig.getFile();
        String string = configuration.getString("spawn");
        if (string == null || string.trim().equals("")) return;

        String[] parts = string.split(":");
        if (parts.length != 6) return;

        String worldName = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        World world = worldManager.getWorld(worldName);
        if (world == null) {
            world = worldManager.loadWorld(worldName);
        }
        world.load();

        this.spawnName = worldName;
        this.spawn = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}
