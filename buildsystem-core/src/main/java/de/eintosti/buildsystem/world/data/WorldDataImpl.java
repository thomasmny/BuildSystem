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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.data.Bypassable;
import de.eintosti.buildsystem.api.data.Overridable;
import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.world.data.type.ConfigurableType;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@NullMarked
public class WorldDataImpl implements WorldData {

    private final Map<String, Type<?>> data = new HashMap<>();
    private String worldName;

    private @Nullable Supplier<@Nullable Folder> folderResolver;

    private final Type<String> customSpawn;
    private final Type<String> permission;
    private final Type<String> project;

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

    private WorldDataImpl(WorldDataBuilder builder) {
        this.worldName = builder.worldName;

        this.customSpawn = register("spawn", new ConfigurableType<>(builder.customSpawn));
        this.permission = register(
                "permission",
                new ConfigurableType<>(builder.permission)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.permission"))
                        .withCapability(Overridable.class, new Overridable<>(builder.permissionOverrideEnabled, () -> {
                            Folder folder = getAssignedFolder();
                            return (folder != null) ? folder.getPermission() : null;
                        })));
        this.project = register(
                "project",
                new ConfigurableType<>(builder.project)
                        .withCapability(Overridable.class, new Overridable<>(builder.projectOverrideEnabled, () -> {
                            Folder folder = getAssignedFolder();
                            return (folder != null) ? folder.getProject() : null;
                        })));

        this.difficulty = register(
                "difficulty", new ConfigurableType<>(builder.difficulty).withConfigFormatter(Difficulty::name));
        this.material =
                register("material", new ConfigurableType<>(builder.material).withConfigFormatter(XMaterial::name));
        this.status = register(
                "status",
                new ConfigurableType<>(builder.status)
                        .withConfigFormatter(BuildWorldStatus::name)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.archive")));

        this.blockBreaking = register(
                "block-breaking",
                new ConfigurableType<>(builder.blockBreaking)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings")));
        this.blockInteractions = register(
                "block-interactions",
                new ConfigurableType<>(builder.blockInteractions)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings")));
        this.blockPlacement = register(
                "block-placement",
                new ConfigurableType<>(builder.blockPlacement)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings")));
        this.buildersEnabled = register("builders-enabled", new ConfigurableType<>(builder.buildersEnabled));
        this.explosions = register("explosions", new ConfigurableType<>(builder.explosions));
        this.mobAi = register("mob-ai", new ConfigurableType<>(builder.mobAi));
        this.physics = register("physics", new ConfigurableType<>(builder.physics));
        this.privateWorld = register("private", new ConfigurableType<>(builder.privateWorld));

        this.timeSinceBackup = register("time-since-backup", new ConfigurableType<>(builder.timeSinceBackup));
        this.lastEdited = register("last-edited", new ConfigurableType<>(builder.lastEdited));
        this.lastLoaded = register("last-loaded", new ConfigurableType<>(builder.lastLoaded));
        this.lastUnloaded = register("last-unloaded", new ConfigurableType<>(builder.lastUnloaded));
    }

    public void setFolderResolver(Supplier<@Nullable Folder> resolver) {
        this.folderResolver = resolver;
    }

    private @Nullable Folder getAssignedFolder() {
        Supplier<@Nullable Folder> resolver = this.folderResolver;
        return resolver != null ? resolver.get() : null;
    }

    private <T> ConfigurableType<T> register(String key, ConfigurableType<T> type) {
        this.data.put(key, type);
        return type;
    }

    @Override
    public Type<String> customSpawn() {
        return customSpawn;
    }

    @Override
    public @Nullable Location getCustomSpawnLocation() {
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
                Float.parseFloat(spawnString[4]));
    }

    @Override
    public Type<String> permission() {
        return permission;
    }

    @Override
    public Type<String> project() {
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

    public static class WorldDataBuilder {

        private final String worldName;

        private String customSpawn = "";
        private String permission = "-";
        private String project = "-";
        private Difficulty difficulty = Difficulty.PEACEFUL;
        private XMaterial material = XMaterial.GRASS_BLOCK;
        private BuildWorldStatus status = BuildWorldStatus.NOT_STARTED;
        private boolean blockBreaking = true;
        private boolean blockInteractions = true;
        private boolean blockPlacement = true;
        private boolean buildersEnabled = false;
        private boolean explosions = true;
        private boolean mobAi = true;
        private boolean physics = true;
        private boolean privateWorld = false;
        private int timeSinceBackup = 0;
        private long lastEdited = -1L;
        private long lastLoaded = -1L;
        private long lastUnloaded = -1L;
        BooleanSupplier permissionOverrideEnabled = () -> false;
        BooleanSupplier projectOverrideEnabled = () -> false;

        /**
         * Creates a new builder for {@link WorldData}.
         *
         * @param worldName The name of the world, which is required.
         */
        public WorldDataBuilder(String worldName) {
            this.worldName = Objects.requireNonNull(worldName, "World name cannot be null");
        }

        /**
         * Builds the final {@link WorldDataImpl} instance.
         *
         * @return A new, immutable {@link WorldDataImpl} object
         */
        public WorldDataImpl build() {
            return new WorldDataImpl(this);
        }

        public WorldDataBuilder withCustomSpawn(String customSpawn) {
            this.customSpawn = customSpawn;
            return this;
        }

        public WorldDataBuilder withPermission(String permission) {
            this.permission = permission;
            return this;
        }

        public WorldDataBuilder withProject(String project) {
            this.project = project;
            return this;
        }

        public WorldDataBuilder withDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public WorldDataBuilder withMaterial(XMaterial material) {
            this.material = material;
            return this;
        }

        public WorldDataBuilder withStatus(BuildWorldStatus status) {
            this.status = status;
            return this;
        }

        public WorldDataBuilder withBlockBreaking(boolean blockBreaking) {
            this.blockBreaking = blockBreaking;
            return this;
        }

        public WorldDataBuilder withBlockInteractions(boolean blockInteractions) {
            this.blockInteractions = blockInteractions;
            return this;
        }

        public WorldDataBuilder withBlockPlacement(boolean blockPlacement) {
            this.blockPlacement = blockPlacement;
            return this;
        }

        public WorldDataBuilder withBuildersEnabled(boolean buildersEnabled) {
            this.buildersEnabled = buildersEnabled;
            return this;
        }

        public WorldDataBuilder withExplosions(boolean explosions) {
            this.explosions = explosions;
            return this;
        }

        public WorldDataBuilder withMobAi(boolean mobAi) {
            this.mobAi = mobAi;
            return this;
        }

        public WorldDataBuilder withPhysics(boolean physics) {
            this.physics = physics;
            return this;
        }

        public WorldDataBuilder withPrivateWorld(boolean privateWorld) {
            this.privateWorld = privateWorld;
            return this;
        }

        public WorldDataBuilder withTimeSinceBackup(int timeSinceBackup) {
            this.timeSinceBackup = timeSinceBackup;
            return this;
        }

        public WorldDataBuilder withLastEdited(long lastEdited) {
            this.lastEdited = lastEdited;
            return this;
        }

        public WorldDataBuilder withLastLoaded(long lastLoaded) {
            this.lastLoaded = lastLoaded;
            return this;
        }

        public WorldDataBuilder withLastUnloaded(long lastUnloaded) {
            this.lastUnloaded = lastUnloaded;
            return this;
        }

        public WorldDataBuilder withPermissionOverrideEnabled(BooleanSupplier supplier) {
            this.permissionOverrideEnabled = supplier;
            return this;
        }

        public WorldDataBuilder withProjectOverrideEnabled(BooleanSupplier supplier) {
            this.projectOverrideEnabled = supplier;
            return this;
        }
    }
}
