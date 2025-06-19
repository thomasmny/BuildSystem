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
import com.google.common.base.Function;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.ConfigValues;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldDataImpl implements WorldData {

    private final ConfigValues configValues;

    private final Map<String, Type<?>> data = new HashMap<>();

    private final Type<String> customSpawn = register("spawn");
    private final Type<String> permission = register("permission");
    private final Type<String> project = register("project");

    private final Type<Difficulty> difficulty = register("difficulty", new DifficultyType());
    private final Type<XMaterial> material = register("material", new MaterialType());
    private final Type<BuildWorldStatus> status = register("status", new StatusType());

    private final Type<Boolean> blockBreaking = register("block-breaking");
    private final Type<Boolean> blockInteractions = register("block-interactions");
    private final Type<Boolean> blockPlacement = register("block-placement");
    private final Type<Boolean> buildersEnabled = register("builders-enabled");
    private final Type<Boolean> explosions = register("explosions");
    private final Type<Boolean> mobAi = register("mob-ai");
    private final Type<Boolean> physics = register("physics");
    private final Type<Boolean> privateWorld = register("private");

    private final Type<Long> lastEdited = register("last-edited");
    private final Type<Long> lastLoaded = register("last-loaded");
    private final Type<Long> lastUnloaded = register("last-unloaded");

    private String worldName;

    public WorldDataImpl(String worldName, boolean privateWorld, XMaterial material, ConfigValues configValues) {
        this(
                worldName,
                null,
                configValues.getDefaultPermission(privateWorld).replace("%world%", worldName),
                "-",
                configValues.getWorldDifficulty(),
                material,
                BuildWorldStatus.NOT_STARTED,
                configValues.isWorldBlockBreaking(),
                configValues.isWorldBlockInteractions(),
                configValues.isWorldBlockPlacement(),
                configValues.isWorldBuildersEnabled(privateWorld),
                configValues.isWorldExplosions(),
                configValues.isWorldMobAi(),
                configValues.isWorldPhysics(),
                privateWorld,
                -1L,
                -1L,
                -1L,
                configValues
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
            long lastLoaded,
            long lastUnloaded,
            long lastEdited,
            ConfigValues configValues
    ) {
        this.customSpawn.set(customSpawn);
        this.permission.set(permission);
        this.project.set(project);

        this.difficulty.set(difficulty);
        this.material.set(material);
        this.status.set(worldStatus);

        this.blockBreaking.set(blockBreaking);
        this.blockInteractions.set(blockInteractions);
        this.blockPlacement.set(blockPlacement);
        this.buildersEnabled.set(buildersEnabled);
        this.explosions.set(explosions);
        this.mobAi.set(mobAi);
        this.physics.set(physics);
        this.privateWorld.set(privateWorld);

        this.lastEdited.set(lastEdited);
        this.lastLoaded.set(lastLoaded);
        this.lastUnloaded.set(lastUnloaded);

        this.worldName = worldName;
        this.configValues = configValues;
    }

    public <T> Type<T> register(@NotNull String key) {
        return register(key, new TypeImpl<>());
    }

    public <T> Type<T> register(@NotNull String key, Type<T> type) {
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
        if (customSpawn == null) {
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

    @Nullable
    private Type<String> getOverrideValue(Function<Folder, String> valueProvider) {
        WorldService worldService = JavaPlugin.getPlugin(BuildSystemPlugin.class).getWorldService();
        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(worldName);
        if (buildWorld != null) {
            Folder assignedFolder = worldService.getFolderStorage().getAssignedFolder(buildWorld);
            if (assignedFolder != null) {
                return new TypeImpl<>(valueProvider.apply(assignedFolder));
            }
        }
        return null;
    }

    @Override
    public Type<String> permission() {
        if (configValues.isFolderOverridePermissions()) {
            Type<String> assignedFolderPermission = getOverrideValue(Folder::getPermission);
            if (assignedFolderPermission != null) {
                return assignedFolderPermission;
            }
        }
        return permission;
    }

    @Override
    public Type<String> project() {
        if (configValues.isFolderOverrideProjects()) {
            Type<String> assignedFolderProject = getOverrideValue(Folder::getProject);
            if (assignedFolderProject != null) {
                return assignedFolderProject;
            }
        }
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

    public static class TypeImpl<T> implements Type<T> {

        private T value;

        public TypeImpl() {
            this.value = null;
        }

        public TypeImpl(T value) {
            this.value = value;
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

    public static class DifficultyType extends TypeImpl<Difficulty> {

        @Override
        public Object getConfigFormat() {
            return super.get().name();
        }
    }

    public static class MaterialType extends TypeImpl<XMaterial> {

        @Override
        public Object getConfigFormat() {
            return super.get().name();
        }
    }

    public static class StatusType extends TypeImpl<BuildWorldStatus> {

        @Override
        public Object getConfigFormat() {
            return super.get().name();
        }
    }
}