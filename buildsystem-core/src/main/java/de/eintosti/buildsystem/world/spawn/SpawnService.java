/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.world.spawn;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.storage.codec.LogoutLocationCodec;
import de.eintosti.buildsystem.storage.yaml.YamlSpawnStorage;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import io.papermc.lib.PaperLib;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SpawnService {

    private final BuildSystemPlugin plugin;
    private final WorldStorage worldStorage;
    private final YamlSpawnStorage spawnStorage;
    private final Executor background;

    private @Nullable String spawnName;
    private @Nullable Location spawn;

    public SpawnService(BuildSystemPlugin plugin, WorldServiceImpl worldService, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.worldStorage = worldService.getWorldStorage();
        this.spawnStorage = new YamlSpawnStorage(plugin);
        this.background = scheduler.background();
        load();
    }

    public boolean teleport(Player player) {
        if (spawn == null || spawnName == null) {
            return false;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(spawnName);
        if (buildWorld != null) {
            if (!buildWorld.isLoaded()) {
                buildWorld.getLoader().loadForPlayer(player);
            }
        }

        player.setFallDistance(0);
        PaperLib.teleportAsync(player, spawn).whenComplete((completed, throwable) -> {
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

    public @Nullable Location getSpawn() {
        return spawn;
    }

    public @Nullable World getSpawnWorld() {
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

    public CompletableFuture<Void> save() {
        return CompletableFuture.runAsync(() -> spawnStorage.saveSpawn(spawn), background);
    }

    private void load() {
        // Same world:x:y:z:yaw:pitch format as a logout location, so the namespaced-name handling is shared.
        LogoutLocation stored = LogoutLocationCodec.parse(spawnStorage.getFile().getString("spawn"));
        if (stored == null) {
            return;
        }

        String worldName = stored.worldName();
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld == null) {
            plugin.getLogger()
                    .warning("Could load spawn world \"" + worldName + "\". Please check logs for possible errors.");
            return;
        }

        buildWorld.getLoader().load();
        this.spawnName = worldName;
        this.spawn = stored.location();
    }
}
