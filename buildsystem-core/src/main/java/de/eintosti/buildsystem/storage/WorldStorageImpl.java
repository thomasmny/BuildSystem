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
import de.eintosti.buildsystem.world.WorldNames;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * In-memory index of the server's {@link BuildWorld}s, keyed both by UUID and by {@link WorldNames#id namespaced
 * name}. Mutations ({@link #addBuildWorld}, {@link #removeBuildWorld}, {@link #rename}) run on the main thread, but
 * lookups may be called off it: {@code AsyncPlayerPreLoginEvent} resolves a returning player's last world on Bukkit's async login thread. The
 * indexes are therefore {@link ConcurrentHashMap}s so concurrent reads stay safe and consistently published, and the
 * compound mutations are ordered so a concurrent reader never observes a world's two index entries out of step.
 */
@NullMarked
public abstract class WorldStorageImpl implements WorldStorage {

    protected final Logger logger;

    private final ConcurrentHashMap<UUID, BuildWorld> buildWorldsByUuid;
    private final ConcurrentHashMap<String, UUID> uuidByName;
    private final Supplier<String> defaultNamespace;

    /**
     * @param defaultNamespace Supplies {@code world.default-namespace}, read on every use so a config reload applies
     */
    protected WorldStorageImpl(Logger logger, Supplier<String> defaultNamespace) {
        this.logger = logger;
        this.defaultNamespace = defaultNamespace;
        this.buildWorldsByUuid = new ConcurrentHashMap<>();
        this.uuidByName = new ConcurrentHashMap<>();
    }

    @Override
    @Nullable @Contract("null -> null")
    public BuildWorld getBuildWorld(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        UUID uuid = this.uuidByName.get(WorldNames.id(name));
        if (uuid == null && WorldNames.isNamespaced(name)) {
            // A namespaced world imported before namespaces existed was stored under its Bukkit name (maps_lobby). A
            // world really named maps_lobby has its own folder; the imported one does not.
            String bukkitName = WorldNames.bukkitName(name);
            UUID legacy = this.uuidByName.get(WorldNames.id(bukkitName));
            if (legacy != null && !FileUtils.hasPlainWorldFolder(bukkitName)) {
                uuid = legacy;
            }
        }
        return uuid == null ? null : this.buildWorldsByUuid.get(uuid);
    }

    @Override
    public @Nullable BuildWorld getBuildWorld(World world) {
        return getBuildWorld(WorldNames.of(world));
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
        this.uuidByName.put(WorldNames.id(buildWorld.getName()), buildWorld.getUniqueId());
    }

    public synchronized void removeBuildWorld(BuildWorld buildWorld) {
        UUID worldId = buildWorld.getUniqueId();
        this.buildWorldsByUuid.remove(worldId);
        this.uuidByName.remove(WorldNames.id(buildWorld.getName()));

        Folder assignedFolder = buildWorld.getFolder();
        if (assignedFolder != null) {
            assignedFolder.removeWorld(buildWorld);
        }
    }

    public synchronized void rename(BuildWorld buildWorld, String oldName, String newName) {
        String oldKey = WorldNames.id(oldName);
        String newKey = WorldNames.id(newName);
        // Publish the new name before dropping the old one so a concurrent (async) reader never sees the world vanish.
        // Guard the removal: a case-only rename maps both names to the same key, which must stay resolvable.
        this.uuidByName.put(newKey, buildWorld.getUniqueId());
        if (!oldKey.equals(newKey)) {
            this.uuidByName.remove(oldKey);
        }
    }

    /**
     * {@return the stored worlds a player means by {@code input}} A name with a namespace matches exactly. A name
     * without one matches that name in any namespace, so old worlds keep working after the default namespace changes;
     * when several match, the one in the default namespace wins, then the one in {@code minecraft}. More than one
     * result means the name is ambiguous.
     */
    public List<BuildWorld> matchWorlds(String input) {
        if (WorldNames.isQualified(input)) {
            BuildWorld buildWorld = getBuildWorld(input);
            return buildWorld == null ? List.of() : List.of(buildWorld);
        }

        List<BuildWorld> matches = worldsWithPath(input);
        if (matches.size() > 1) {
            for (String namespace : List.of(defaultNamespace.get(), NamespacedKey.MINECRAFT)) {
                for (BuildWorld match : matches) {
                    if (WorldNames.namespace(match.getName()).equals(namespace)) {
                        return List.of(match);
                    }
                }
            }
        }
        return matches;
    }

    /**
     * {@return the shortest name a player can type for an existing world} That is its name without a namespace when no
     * other world shares it, otherwise {@code namespace:name}.
     */
    public String typedName(String worldName) {
        return worldsWithPath(WorldNames.path(worldName)).size() > 1
                ? WorldNames.qualified(worldName)
                : WorldNames.path(worldName);
    }

    /**
     * {@return the name of a new world a player typed} A name without a namespace is placed in the default namespace.
     */
    public String newWorldName(String input) {
        return WorldNames.fromInput(input, defaultNamespace.get());
    }

    /**
     * {@return what a player types to give a new world this name} The inverse of {@link #newWorldName}.
     */
    public String typedNewName(String worldName) {
        return WorldNames.namespace(worldName).equals(defaultNamespace.get())
                ? WorldNames.path(worldName)
                : WorldNames.qualified(worldName);
    }

    private List<BuildWorld> worldsWithPath(String path) {
        return getBuildWorlds().stream()
                .filter(buildWorld -> WorldNames.path(buildWorld.getName()).equalsIgnoreCase(path))
                .toList();
    }

    /**
     * {@return the name of the world a new world named {@code worldName} would clash with, or {@code null}} Paper names
     * the world {@code maps:lobby} {@code maps_lobby}, so it cannot exist next to a world really named
     * {@code maps_lobby}. A loaded world counts too, unless it is the very world being named, as when importing a world
     * another plugin loaded.
     */
    public @Nullable String bukkitNameClash(String worldName) {
        String id = WorldNames.id(worldName);
        String bukkitName = WorldNames.bukkitName(worldName);
        for (BuildWorld buildWorld : getBuildWorlds()) {
            String name = buildWorld.getName();
            if (!WorldNames.id(name).equals(id) && WorldNames.bukkitName(name).equalsIgnoreCase(bukkitName)) {
                return name;
            }
        }
        World loaded = Bukkit.getWorld(bukkitName);
        if (loaded != null && !WorldNames.id(WorldNames.of(loaded)).equals(id)) {
            return WorldNames.of(loaded);
        }
        return null;
    }

    @Override
    public boolean worldExists(String worldName) {
        return getBuildWorld(worldName) != null;
    }

    /**
     * {@return the names of the world folders under the main level's dimensions that are not imported yet}
     */
    public List<String> unimportedWorldNames() {
        return FileUtils.dimensionWorldNames().stream()
                .filter(name -> !worldExists(name))
                .toList();
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
