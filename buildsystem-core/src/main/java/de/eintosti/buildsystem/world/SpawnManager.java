/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.world;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.yaml.YamlSpawnStorage;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SpawnManager {

    private final BuildSystemPlugin plugin;
    private final WorldStorage worldStorage;
    private final YamlSpawnStorage spawnStorage;

    @Nullable
    private String spawnName;
    @Nullable
    private Location spawn;

    public SpawnManager(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.spawnStorage = new YamlSpawnStorage(plugin);
        load();
    }

    public boolean teleport(Player player) {
        if (!spawnExists()) {
            return false;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(spawnName);
        if (buildWorld != null) {
            if (!buildWorld.isLoaded()) {
                buildWorld.getLoader().loadForPlayer(player);
            }
        }

        player.setFallDistance(0);
        PaperLib.teleportAsync(player, spawn)
                .whenComplete((completed, throwable) -> {
                    if (!completed) {
                        return;
                    }
                    XSound.ENTITY_ZOMBIE_INFECT.play(player);
                    player.resetTitle();
                });
        return true;
    }

    public boolean spawnExists() {
        return spawn != null;
    }

    @Nullable
    public Location getSpawn() {
        return spawn;
    }

    @Nullable
    public World getSpawnWorld() {
        if (this.spawn == null) {
            return null;
        }
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
        spawnStorage.saveSpawn(spawn);
    }

    private void load() {
        FileConfiguration configuration = spawnStorage.getFile();
        String string = configuration.getString("spawn");
        if (string == null || string.trim().isEmpty()) {
            return;
        }

        String[] parts = string.split(":");
        if (parts.length != 6) {
            return;
        }

        String worldName = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld == null) {
            plugin.getLogger().warning("Could load spawn world \"" + worldName + "\". Please check logs for possible errors.");
            return;
        }

        buildWorld.getLoader().load();
        this.spawnName = worldName;
        this.spawn = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}