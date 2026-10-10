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
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import io.papermc.lib.PaperLib;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Holds the server spawn as a world name plus coordinates. The world is only resolved when the spawn is used: the spawn
 * is read while the plugin enables, before any build world is registered, and a held {@link org.bukkit.World} would go
 * stale once its world unloads.
 */
@NullMarked
public class SpawnService {

    private final BuildSystemPlugin plugin;
    private final WorldStorage worldStorage;
    private final YamlSpawnStorage spawnStorage;
    private final Executor background;

    private @Nullable LogoutLocation spawn;

    /**
     * Set when the spawn changes. A stored spawn that fails to parse loads as no spawn, and writing that back on the
     * next periodic save would erase it, so the file is only written after a change.
     */
    private volatile boolean dirty;

    public SpawnService(BuildSystemPlugin plugin, WorldServiceImpl worldService, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.worldStorage = worldService.getWorldStorage();
        this.spawnStorage = new YamlSpawnStorage(plugin);
        this.background = scheduler.background();
        // Same world:x:y:z:yaw:pitch format as a logout location, so the namespaced-name handling is shared.
        this.spawn = LogoutLocationCodec.parse(spawnStorage.getSpawn());
    }

    public boolean teleport(Player player) {
        LogoutLocation stored = this.spawn;
        if (stored == null) {
            return false;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(stored.worldName());
        if (buildWorld != null && !buildWorld.isLoaded()) {
            buildWorld.getLoader().loadForPlayer(player);
        }

        Location location = stored.location();
        if (location == null) {
            return false;
        }

        player.setFallDistance(0);
        PaperLib.teleportAsync(player, location).whenComplete((completed, throwable) -> {
            if (!completed) {
                return;
            }
            XSound.ENTITY_ZOMBIE_INFECT.play(player);
            player.resetTitle();
        });
        return true;
    }

    /**
     * Loads the spawn's build world, so the spawn can be used straight away. Called once the stored worlds have been
     * registered at startup.
     */
    public void loadSpawnWorld() {
        LogoutLocation stored = this.spawn;
        if (stored == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(stored.worldName());
        if (buildWorld == null) {
            plugin.getLogger()
                    .warning("Could not load spawn world \"" + stored.worldName()
                            + "\". Please check logs for possible errors.");
            return;
        }
        buildWorld.getLoader().load();
    }

    public boolean spawnExists() {
        return spawn != null;
    }

    /**
     * {@return the spawn location, or {@code null} when no spawn is set or its world is not loaded}
     */
    public @Nullable Location getSpawn() {
        LogoutLocation stored = this.spawn;
        return stored != null ? stored.location() : null;
    }

    /**
     * {@return whether the spawn is set inside the given world}
     */
    public boolean isIn(String worldName) {
        LogoutLocation stored = this.spawn;
        return stored != null && WorldNames.id(stored.worldName()).equals(WorldNames.id(worldName));
    }

    public void set(Location location, String worldName) {
        this.spawn = new LogoutLocation(worldName, location);
        this.dirty = true;
    }

    /**
     * Keeps the spawn in a renamed world, at the same coordinates.
     */
    public void renameWorld(String oldName, String newName) {
        LogoutLocation stored = this.spawn;
        if (stored != null && isIn(oldName)) {
            this.spawn = stored.withWorldName(newName);
            this.dirty = true;
        }
    }

    public void remove() {
        this.spawn = null;
        this.dirty = true;
    }

    /**
     * Writes the spawn when it changed since the last save.
     */
    public CompletableFuture<Void> save() {
        if (!dirty) {
            return CompletableFuture.completedFuture(null);
        }
        dirty = false;
        LogoutLocation stored = this.spawn;
        String formatted = stored != null ? LogoutLocationCodec.format(stored) : null;
        return CompletableFuture.runAsync(() -> spawnStorage.saveSpawn(formatted), background);
    }
}
