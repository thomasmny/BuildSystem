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
package de.eintosti.buildsystem.storage;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.util.FileUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * In-memory index of the server's {@link BuildWorld}s, keyed both by UUID and by lower-cased name. Mutations
 * ({@link #addBuildWorld}, {@link #removeBuildWorld}, {@link #rename}) run on the main thread, but lookups may be called
 * off it: {@code AsyncPlayerPreLoginEvent} resolves a returning player's last world on Bukkit's async login thread. The
 * indexes are therefore {@link ConcurrentHashMap}s so concurrent reads stay safe and consistently published, and the
 * compound mutations are ordered so a concurrent reader never observes a world's two index entries out of step.
 */
@NullMarked
public abstract class WorldStorageImpl implements WorldStorage {

    protected final Logger logger;

    private final ConcurrentHashMap<UUID, BuildWorld> buildWorldsByUuid;
    private final ConcurrentHashMap<String, UUID> uuidByName;

    protected WorldStorageImpl(Logger logger) {
        this.logger = logger;
        this.buildWorldsByUuid = new ConcurrentHashMap<>();
        this.uuidByName = new ConcurrentHashMap<>();
    }

    @Override
    @Nullable @Contract("null -> null")
    public BuildWorld getBuildWorld(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        UUID uuid = this.uuidByName.get(name.toLowerCase());
        if (uuid != null) {
            return this.buildWorldsByUuid.get(uuid);
        }

        return null;
    }

    @Override
    public @Nullable BuildWorld getBuildWorld(World world) {
        return getBuildWorld(world.getName());
    }

    @Override
    public @Nullable BuildWorld getBuildWorld(UUID uuid) {
        return this.buildWorldsByUuid.get(uuid);
    }

    @Override
    @Unmodifiable
    public Collection<BuildWorld> getBuildWorlds() {
        return Collections.unmodifiableCollection(buildWorldsByUuid.values());
    }

    public synchronized void addBuildWorld(BuildWorld buildWorld) {
        this.buildWorldsByUuid.put(buildWorld.getUniqueId(), buildWorld);
        this.uuidByName.put(buildWorld.getName().toLowerCase(), buildWorld.getUniqueId());
    }

    public synchronized void removeBuildWorld(BuildWorld buildWorld) {
        UUID worldId = buildWorld.getUniqueId();
        this.buildWorldsByUuid.remove(worldId);
        this.uuidByName.remove(buildWorld.getName().toLowerCase());

        Folder assignedFolder = buildWorld.getFolder();
        if (assignedFolder != null) {
            assignedFolder.removeWorld(buildWorld);
        }
    }

    public synchronized void rename(BuildWorld buildWorld, String oldName, String newName) {
        String oldKey = oldName.toLowerCase();
        String newKey = newName.toLowerCase();
        // Publish the new name before dropping the old one so a concurrent (async) reader never sees the world vanish.
        // Guard the removal: a case-only rename maps both names to the same key, which must stay resolvable.
        this.uuidByName.put(newKey, buildWorld.getUniqueId());
        if (!oldKey.equals(newKey)) {
            this.uuidByName.remove(oldKey);
        }
    }

    @Override
    public boolean worldExists(String worldName) {
        return getBuildWorld(worldName) != null;
    }

    @Override
    public boolean worldAndFolderExist(String worldName) {
        boolean worldExists = worldExists(worldName);
        if (!worldExists) {
            return false;
        }

        return FileUtils.worldFolder(worldName).isDirectory();
    }

    @Override
    @Unmodifiable
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player) {
        return getBuildWorlds().stream()
                .filter(buildWorld -> buildWorld.getBuilders().isCreator(player))
                .toList();
    }

    @Override
    @Unmodifiable
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player, Visibility visibility) {
        return getBuildWorldsCreatedByPlayer(player).stream()
                .filter(buildWorld -> buildWorld.getData().get(WorldDataKey.VISIBILITY) == visibility)
                .toList();
    }
}
