/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.config.ConfigValues;
import org.bukkit.Difficulty;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class WorldData implements ConfigurationSerializable {

    public final Type<String> CUSTOM_SPAWN = register("spawn");
    public final Type<String> PERMISSION = register("permission");
    public final Type<String> PROJECT = register("project");

    public final Type<Difficulty> DIFFICULTY = register("difficulty");
    public final Type<XMaterial> MATERIAL = register("material");
    public final Type<WorldStatus> STATUS = register("status");

    public final Type<Boolean> BLOCK_BREAKING = register("block-breaking");
    public final Type<Boolean> BLOCK_INTERACTIONS = register("block-interactions");
    public final Type<Boolean> BLOCK_PLACEMENT = register("block-placement");
    public final Type<Boolean> BUILDERS_ENABLED = register("builders-enabled");
    public final Type<Boolean> EXPLOSIONS = register("explosions");
    public final Type<Boolean> MOB_AI = register("mob-ai");
    public final Type<Boolean> PHYSICS = register("physics");
    public final Type<Boolean> PRIVATE = register("private");

    public final Type<Long> LAST_EDITED = register("last-edited");
    public final Type<Long> LAST_LOADED = register("last-loaded");
    public final Type<Long> LAST_UNLOADED = register("last-unloaded");

    private final Map<String, Type<?>> data = new HashMap<>();

    public <T> Type<T> register(@NotNull String key) {
        Type<T> type = new Type<>();
        this.data.put(key, type);
        return type;
    }

    public WorldData(String name, ConfigValues configValues, boolean privateWorld) {
        this.CUSTOM_SPAWN.set(null);
        this.PERMISSION.set(configValues.getDefaultPermission(privateWorld).replace("%world%", name));
        this.PROJECT.set("-");

        this.DIFFICULTY.set(configValues.getWorldDifficulty());
        this.STATUS.set(WorldStatus.NOT_STARTED);

        this.BLOCK_BREAKING.set(configValues.isWorldBlockBreaking());
        this.BLOCK_INTERACTIONS.set(configValues.isWorldBlockInteractions());
        this.BLOCK_PLACEMENT.set(configValues.isWorldBlockPlacement());
        this.BUILDERS_ENABLED.set(configValues.isWorldBuildersEnabled(privateWorld));
        this.EXPLOSIONS.set(configValues.isWorldExplosions());
        this.MOB_AI.set(configValues.isWorldMobAi());
        this.PHYSICS.set(configValues.isWorldPhysics());

        this.LAST_EDITED.set((long) -1);
        this.LAST_LOADED.set((long) -1);
        this.LAST_UNLOADED.set((long) -1);
    }

    public WorldData(
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
        this.CUSTOM_SPAWN.set(customSpawn);
        this.PERMISSION.set(permission);
        this.PROJECT.set(project);

        this.DIFFICULTY.set(difficulty);
        this.MATERIAL.set(material);
        this.STATUS.set(worldStatus);

        this.BLOCK_BREAKING.set(blockBreaking);
        this.BLOCK_INTERACTIONS.set(blockInteractions);
        this.BLOCK_PLACEMENT.set(blockPlacement);
        this.BUILDERS_ENABLED.set(buildersEnabled);
        this.EXPLOSIONS.set(explosions);
        this.MOB_AI.set(mobAi);
        this.PHYSICS.set(physics);
        this.PRIVATE.set(privateWorld);

        this.LAST_EDITED.set(lastEdited);
        this.LAST_LOADED.set(lastLoaded);
        this.LAST_UNLOADED.set(lastUnloaded);
    }

    public static class Type<T> {

        private T value;

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return data.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}