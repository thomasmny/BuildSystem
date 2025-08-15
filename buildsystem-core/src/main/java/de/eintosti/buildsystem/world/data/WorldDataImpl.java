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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.config.Config.World.Default;
import de.eintosti.buildsystem.config.Config.World.Default.Permission;
import de.eintosti.buildsystem.config.Config.World.Default.Settings;
import de.eintosti.buildsystem.config.Config.World.Default.Settings.BuildersEnabled;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldDataImpl implements WorldData {

    private final Map<String, Type<?>> data = new HashMap<>();

    private final Type<String> customSpawn;
    private final OverridableType<String> permission;
    private final OverridableType<String> project;

    private final Type<Difficulty> difficulty;
    private final Type<XMaterial> material;
    private final Type<BuildWorldStatus> status;

    private final Type<Boolean> blockBreaking;
    private final Type<Boolean> blockInteractions;
    private final Type<Boolean> blockPlacement;
    private final Type<Boolean> buildersEnabled;
    private final Type<Boolean> explosions;
    private final Type<Boolean> mobAi;
    private final Type<Boolean> physics;
    private final Type<Boolean> privateWorld;

    private final Type<Integer> timeSinceBackup;

    private final Type<Long> lastEdited;
    private final Type<Long> lastLoaded;
    private final Type<Long> lastUnloaded;

    private String worldName;

    public WorldDataImpl(String worldName, boolean privateWorld, XMaterial material) {
        this(
                worldName,
                "",
                (privateWorld ? Permission.privatePermission : Permission.publicPermission).replace("%world%", worldName),
                "-",
                Default.difficulty,
                material,
                BuildWorldStatus.NOT_STARTED,
                Settings.blockBreaking,
                Settings.blockInteractions,
                Settings.blockPlacement,
                (privateWorld ? BuildersEnabled.privateBuilders : BuildersEnabled.publicBuilders),
                Settings.explosions,
                Settings.mobAi,
                Settings.physics,
                privateWorld,
                0,
                -1L,
                -1L,
                -1L
        );
    }

    public WorldDataImpl(
            String worldName,
            String customSpawn,
            String permission,
            String project,
            Difficulty difficulty,
            XMaterial material,
            BuildWorldStatus worldStatus,
            boolean blockBreaking,
            boolean blockInteractions,
            boolean blockPlacement,
            boolean buildersEnabled,
            boolean explosions,
            boolean mobAi,
            boolean physics,
            boolean privateWorld,
            int timeSinceBackup,
            long lastLoaded,
            long lastUnloaded,
            long lastEdited
    ) {
        this.customSpawn = register("spawn", customSpawn);
        this.permission = registerOverridable("permission", permission, Config.Folder.overridePermissions, worldName, Folder::getPermission);
        this.project = registerOverridable("project", project, Config.Folder.overrideProjects, worldName, Folder::getProject);

        this.difficulty = register("difficulty", new DifficultyType(difficulty));
        this.material = register("material", new MaterialType(material));
        this.status = register("status", new StatusType(worldStatus));

        this.blockBreaking = register("block-breaking", blockBreaking);
        this.blockInteractions = register("block-interactions", blockInteractions);
        this.blockPlacement = register("block-placement", blockPlacement);
        this.buildersEnabled = register("builders-enabled", buildersEnabled);
        this.explosions = register("explosions", explosions);
        this.mobAi = register("mob-ai", mobAi);
        this.physics = register("physics", physics);
        this.privateWorld = register("private", privateWorld);

        this.timeSinceBackup = register("time-since-backup", timeSinceBackup);

        this.lastEdited = register("last-edited", lastEdited);
        this.lastLoaded = register("last-loaded", lastLoaded);
        this.lastUnloaded = register("last-unloaded", lastUnloaded);

        this.worldName = worldName;
    }

    /**
     * Registers a new {@link Type} instance with a given key.
     *
     * @param key  The string identifier for the data type
     * @param type The {@link Type} instance to register
     * @param <T>  The type of the value held by the {@link Type}
     * @return The registered type instance
     */
    public <T> Type<T> register(String key, Type<T> type) {
        this.data.put(key, type);
        return type;
    }

    /**
     * Registers a new {@link Type} instance with a given key and a default value.
     *
     * @param key          The string identifier for the data type
     * @param defaultValue The initial default value for this type
     * @param <T>          The type of the value
     * @return The registered type instance
     */
    public <T> Type<T> register(String key, T defaultValue) {
        Type<T> type = new TypeImpl<>(defaultValue);
        this.data.put(key, type);
        return type;
    }

    /**
     * Registers a new {@link OverridableType} instance with a given key, default value, override enablement flag, world name, and a function to provide the override value from a
     * {@link Folder}.
     *
     * @param key              The string identifier for the overridable data type
     * @param defaultValue     The initial default value for this type
     * @param enableOverride   Whether overriding this values is to be enabled or not
     * @param worldName        The name of the world this data belongs to, used for context in override resolution
     * @param overrideProvider A {@link Function} that, given a {@link Folder}, returns the overridden value
     * @param <T>              The type of the value
     * @return The registered type instance
     */
    public <T> OverridableType<T> registerOverridable(String key, T defaultValue, boolean enableOverride, String worldName, Function<Folder, T> overrideProvider) {
        OverridableType<T> type = new OverridableType<>(defaultValue, enableOverride, worldName, overrideProvider);
        this.data.put(key, type);
        return type;
    }

    @Override
    public Type<String> customSpawn() {
        return customSpawn;
    }

    @Override
    @Nullable
    public Location getCustomSpawnLocation() {
        String customSpawn = customSpawn().get();
        if (customSpawn.isBlank()) {
            return null;
        }

        String[] spawnString = customSpawn.split(";");
        return new Location(
                Bukkit.getWorld(worldName),
                Double.parseDouble(spawnString[0]),
                Double.parseDouble(spawnString[1]),
                Double.parseDouble(spawnString[2]),
                Float.parseFloat(spawnString[3]),
                Float.parseFloat(spawnString[4])
        );
    }

    @Override
    public OverridableType<String> permission() {
        return permission;
    }

    @Override
    public OverridableType<String> project() {
        return project;
    }

    @Override
    public Type<Difficulty> difficulty() {
        return difficulty;
    }

    @Override
    public Type<XMaterial> material() {
        return material;
    }

    @Override
    public Type<BuildWorldStatus> status() {
        return status;
    }

    @Override
    public Type<Boolean> blockBreaking() {
        return blockBreaking;
    }

    @Override
    public Type<Boolean> blockInteractions() {
        return blockInteractions;
    }

    @Override
    public Type<Boolean> blockPlacement() {
        return blockPlacement;
    }

    @Override
    public Type<Boolean> buildersEnabled() {
        return buildersEnabled;
    }

    @Override
    public Type<Boolean> explosions() {
        return explosions;
    }

    @Override
    public Type<Boolean> mobAi() {
        return mobAi;
    }

    @Override
    public Type<Boolean> physics() {
        return physics;
    }

    @Override
    public Type<Boolean> privateWorld() {
        return privateWorld;
    }

    @Override
    public Type<Integer> timeSinceBackup() {
        return timeSinceBackup;
    }

    @Override
    public Type<Long> lastEdited() {
        return lastEdited;
    }

    @Override
    public Type<Long> lastLoaded() {
        return lastLoaded;
    }

    @Override
    public Type<Long> lastUnloaded() {
        return lastUnloaded;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public Map<String, Type<?>> getAllData() {
        return data;
    }

    /**
     * A basic implementation of the {@link Type} interface. It holds a single value of a generic type {@code T} and provides methods to get and set its value.
     *
     * @param <T> The type of the value held by this {@link TypeImpl}
     */
    public static class TypeImpl<T> implements Type<T> {

        private T value;

        public TypeImpl(T defaultValue) {
            this.value = defaultValue;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public void set(T value) {
            this.value = value;
        }

        @Override
        public Object getConfigFormat() {
            return value;
        }
    }

    /**
     * An extension of {@link TypeImpl} that allows its value to be overridden dynamically based on a {@link Folder}'s settings.
     * <p>
     * The override behavior is enabled or disabled by a configuration flag. If enabled and an associated folder exists, the value is retrieved from the folder; otherwise, it falls
     * back to the locally stored value.
     *
     * @param <T> The type of the value held by this overridable type
     */
    public static class OverridableType<T> extends TypeImpl<T> {

        private final boolean enabledOverride;
        private final String worldName;
        private final Function<Folder, T> overrideProvider;

        public OverridableType(T defaultValue, boolean enabledOverride, String worldName, Function<Folder, T> overrideProvider) {
            super(defaultValue);
            this.enabledOverride = enabledOverride;
            this.worldName = worldName;
            this.overrideProvider = overrideProvider;
        }

        @Override
        public T get() {
            if (!enabledOverride) {
                return super.get();
            }

            WorldService worldService = BuildSystemPlugin.get().getWorldService();
            BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(this.worldName);
            if (buildWorld != null) {
                Folder assignedFolder = buildWorld.getFolder();
                if (assignedFolder != null) {
                    return this.overrideProvider.apply(assignedFolder);
                }
            }

            return super.get();
        }
    }

    /**
     * A specific {@link TypeImpl} for handling {@link Difficulty} values.
     * <p>
     * Overrides {@link #getConfigFormat()} to return the difficulty's name.
     */
    public static class DifficultyType extends TypeImpl<Difficulty> {

        public DifficultyType(Difficulty difficulty) {
            super(difficulty);
        }

        @Override
        public Object getConfigFormat() {
            return super.get().name();
        }
    }

    /**
     * A specific {@link TypeImpl} for handling {@link XMaterial} values.
     * <p>
     * Overrides {@link #getConfigFormat()} to return the material's name.
     */
    public static class MaterialType extends TypeImpl<XMaterial> {

        public MaterialType(XMaterial material) {
            super(material);
        }

        @Override
        public Object getConfigFormat() {
            return super.get().name();
        }
    }

    /**
     * A specific {@link TypeImpl} for handling {@link BuildWorldStatus} values.
     * <p>
     * Overrides {@link #getConfigFormat()} to return the status's name.
     */
    public static class StatusType extends TypeImpl<BuildWorldStatus> {

        public StatusType(BuildWorldStatus status) {
            super(status);
        }

        @Override
        public Object getConfigFormat() {
            return super.get().name();
        }
    }
}