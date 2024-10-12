/*
 * Copyright (c) 2018-2024, Thomas Meaney
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
import de.eintosti.buildsystem.config.ConfigValues;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldData implements ConfigurationSerializable {

    private final Map<String, Type<?>> data = new HashMap<>();

    private final Type<String> customSpawn = register("spawn");
    private final Type<String> permission = register("permission");
    private final Type<String> project = register("project");

    private final Type<Difficulty> difficulty = register("difficulty", new DifficultyType());
    private final Type<XMaterial> material = register("material", new MaterialType());
    private final Type<WorldStatus> status = register("status", new StatusType());

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

    public WorldData(String worldName, ConfigValues configValues, boolean privateWorld) {
        this.customSpawn.set(null);
        this.permission.set(configValues.getDefaultPermission(privateWorld).replace("%world%", worldName));
        this.project.set("-");

        this.difficulty.set(configValues.getWorldDifficulty());
        this.status.set(WorldStatus.NOT_STARTED);

        this.blockBreaking.set(configValues.isWorldBlockBreaking());
        this.blockInteractions.set(configValues.isWorldBlockInteractions());
        this.blockPlacement.set(configValues.isWorldBlockPlacement());
        this.buildersEnabled.set(configValues.isWorldBuildersEnabled(privateWorld));
        this.explosions.set(configValues.isWorldExplosions());
        this.mobAi.set(configValues.isWorldMobAi());
        this.physics.set(configValues.isWorldPhysics());
        this.privateWorld.set(privateWorld);

        this.lastEdited.set((long) -1);
        this.lastLoaded.set((long) -1);
        this.lastUnloaded.set((long) -1);

        this.worldName = worldName;
    }

    public WorldData(
            String worldName,
            String customSpawn,
            String permission,
            String project,
            Difficulty difficulty,
            XMaterial material,
            WorldStatus worldStatus,
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
            long lastEdited
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
    }

  public <T> Type<T> register(@NotNull String key) {
    return register(key, new Type<>());
  }

  public <T> Type<T> register(@NotNull String key, Type<T> type) {
    this.data.put(key, type);
    return type;
  }

    public Type<String> customSpawn() {
        return customSpawn;
    }

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

    public Type<String> permission() {
        return permission;
    }

    public Type<String> project() {
        return project;
    }

    public Type<Difficulty> difficulty() {
        return difficulty;
    }

    public Type<XMaterial> material() {
        return material;
    }

    public Type<WorldStatus> status() {
        return status;
    }

    public Type<Boolean> blockBreaking() {
        return blockBreaking;
    }

    public Type<Boolean> blockInteractions() {
        return blockInteractions;
    }

    public Type<Boolean> blockPlacement() {
        return blockPlacement;
    }

    public Type<Boolean> buildersEnabled() {
        return buildersEnabled;
    }

    public Type<Boolean> explosions() {
        return explosions;
    }

    public Type<Boolean> mobAi() {
        return mobAi;
    }

    public Type<Boolean> physics() {
        return physics;
    }

    public Type<Boolean> privateWorld() {
        return privateWorld;
    }

    public Type<Long> lastEdited() {
        return lastEdited;
    }

    public Type<Long> lastLoaded() {
        return lastLoaded;
    }

    public Type<Long> lastUnloaded() {
        return lastUnloaded;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return data.entrySet().stream()
                .filter(entry -> entry.getValue().get() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getConfigFormat()));
    }

    public static class Type<T> {

      private T value;

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }

        protected Object getConfigFormat() {
            return value;
        }
    }

    public static class DifficultyType extends Type<Difficulty> {

      @Override
        protected Object getConfigFormat() {
            return super.get().name();
        }
    }

    public static class MaterialType extends Type<XMaterial> {

      @Override
        protected Object getConfigFormat() {
            return super.get().name();
        }
    }

    public static class StatusType extends Type<WorldStatus> {

      @Override
        protected Object getConfigFormat() {
            return super.get().name();
        }
    }
}