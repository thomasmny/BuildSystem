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
import de.eintosti.buildsystem.api.data.Property;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.world.data.type.ConfigurableProperty;
import de.eintosti.buildsystem.world.data.type.PersistentProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldDataImpl implements WorldData {

    private final Map<String, PersistentProperty<?>> data = new HashMap<>();
    private String worldName;

    private @Nullable Supplier<@Nullable Folder> folderResolver;

    private final Property<String> customSpawn;
    private final Property<String> permission;
    private final Property<String> project;

    private final Property<Difficulty> difficulty;
    private final Property<XMaterial> material;
    private final Property<BuildWorldStatus> status;

    private final Property<Boolean> blockBreaking;
    private final Property<Boolean> blockInteractions;
    private final Property<Boolean> blockPlacement;
    private final Property<Boolean> buildersEnabled;
    private final Property<Boolean> explosions;
    private final Property<Boolean> mobAi;
    private final Property<Boolean> physics;
    private final Property<Boolean> privateWorld;

    private final Property<Integer> timeSinceBackup;
    private final Property<Long> lastEdited;
    private final Property<Long> lastLoaded;
    private final Property<Long> lastUnloaded;

    private WorldDataImpl(WorldDataBuilder builder) {
        this.worldName = builder.worldName;

        this.customSpawn = register("spawn", new ConfigurableProperty<>(builder.customSpawn));
        this.permission = register(
                "permission",
                new ConfigurableProperty<>(builder.permission)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.permission"))
                        .withCapability(Overridable.class, new Overridable<>(builder.permissionOverrideEnabled, () -> {
                            Folder folder = getAssignedFolder();
                            return (folder != null) ? folder.getPermission() : null;
                        })));
        this.project = register(
                "project",
                new ConfigurableProperty<>(builder.project)
                        .withCapability(Overridable.class, new Overridable<>(builder.projectOverrideEnabled, () -> {
                            Folder folder = getAssignedFolder();
                            return (folder != null) ? folder.getProject() : null;
                        })));

        this.difficulty = register(
                "difficulty", new ConfigurableProperty<>(builder.difficulty).withConfigFormatter(Difficulty::name));
        this.material =
                register("material", new ConfigurableProperty<>(builder.material).withConfigFormatter(XMaterial::name));
        this.status = register(
                "status",
                new ConfigurableProperty<>(builder.status)
                        .withConfigFormatter(BuildWorldStatus::name)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.archive")));

        this.blockBreaking = register(
                "block-breaking",
                new ConfigurableProperty<>(builder.blockBreaking)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings")));
        this.blockInteractions = register(
                "block-interactions",
                new ConfigurableProperty<>(builder.blockInteractions)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings")));
        this.blockPlacement = register(
                "block-placement",
                new ConfigurableProperty<>(builder.blockPlacement)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings")));
        this.buildersEnabled = register("builders-enabled", new ConfigurableProperty<>(builder.buildersEnabled));
        this.explosions = register("explosions", new ConfigurableProperty<>(builder.explosions));
        this.mobAi = register("mob-ai", new ConfigurableProperty<>(builder.mobAi));
        this.physics = register("physics", new ConfigurableProperty<>(builder.physics));
        this.privateWorld = register("private", new ConfigurableProperty<>(builder.privateWorld));

        this.timeSinceBackup = register("time-since-backup", new ConfigurableProperty<>(builder.timeSinceBackup));
        this.lastEdited = register("last-edited", new ConfigurableProperty<>(builder.lastEdited));
        this.lastLoaded = register("last-loaded", new ConfigurableProperty<>(builder.lastLoaded));
        this.lastUnloaded = register("last-unloaded", new ConfigurableProperty<>(builder.lastUnloaded));
    }

    public void setFolderResolver(Supplier<@Nullable Folder> resolver) {
        this.folderResolver = resolver;
    }

    private @Nullable Folder getAssignedFolder() {
        Supplier<@Nullable Folder> resolver = this.folderResolver;
        return resolver != null ? resolver.get() : null;
    }

    private <T> ConfigurableProperty<T> register(String key, ConfigurableProperty<T> property) {
        this.data.put(key, property);
        return property;
    }

    @Override
    public Property<String> customSpawn() {
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
    public Property<String> permission() {
        return permission;
    }

    @Override
    public String getPermission() {
        return permission.get();
    }

    @Override
    public void setPermission(String permission) {
        this.permission.set(permission);
    }

    @Override
    public Property<String> project() {
        return project;
    }

    @Override
    public String getProject() {
        return project.get();
    }

    @Override
    public void setProject(String project) {
        this.project.set(project);
    }

    @Override
    public Property<Difficulty> difficulty() {
        return difficulty;
    }

    @Override
    public Difficulty getDifficulty() {
        return difficulty.get();
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty.set(difficulty);
    }

    @Override
    public Property<XMaterial> material() {
        return material;
    }

    @Override
    public XMaterial getMaterial() {
        return material.get();
    }

    @Override
    public void setMaterial(XMaterial material) {
        this.material.set(material);
    }

    @Override
    public Property<BuildWorldStatus> status() {
        return status;
    }

    @Override
    public BuildWorldStatus getStatus() {
        return status.get();
    }

    @Override
    public void setStatus(BuildWorldStatus status) {
        this.status.set(status);
    }

    @Override
    public Property<Boolean> blockBreaking() {
        return blockBreaking;
    }

    @Override
    public boolean isBlockBreaking() {
        return blockBreaking.get();
    }

    @Override
    public void setBlockBreaking(boolean blockBreaking) {
        this.blockBreaking.set(blockBreaking);
    }

    @Override
    public Property<Boolean> blockInteractions() {
        return blockInteractions;
    }

    @Override
    public boolean isBlockInteractions() {
        return blockInteractions.get();
    }

    @Override
    public void setBlockInteractions(boolean blockInteractions) {
        this.blockInteractions.set(blockInteractions);
    }

    @Override
    public Property<Boolean> blockPlacement() {
        return blockPlacement;
    }

    @Override
    public boolean isBlockPlacement() {
        return blockPlacement.get();
    }

    @Override
    public void setBlockPlacement(boolean blockPlacement) {
        this.blockPlacement.set(blockPlacement);
    }

    @Override
    public Property<Boolean> buildersEnabled() {
        return buildersEnabled;
    }

    @Override
    public boolean isBuildersEnabled() {
        return buildersEnabled.get();
    }

    @Override
    public void setBuildersEnabled(boolean buildersEnabled) {
        this.buildersEnabled.set(buildersEnabled);
    }

    @Override
    public Property<Boolean> explosions() {
        return explosions;
    }

    @Override
    public boolean isExplosions() {
        return explosions.get();
    }

    @Override
    public void setExplosions(boolean explosions) {
        this.explosions.set(explosions);
    }

    @Override
    public Property<Boolean> mobAi() {
        return mobAi;
    }

    @Override
    public boolean isMobAi() {
        return mobAi.get();
    }

    @Override
    public void setMobAi(boolean mobAi) {
        this.mobAi.set(mobAi);
    }

    @Override
    public Property<Boolean> physics() {
        return physics;
    }

    @Override
    public boolean isPhysics() {
        return physics.get();
    }

    @Override
    public void setPhysics(boolean physics) {
        this.physics.set(physics);
    }

    @Override
    public Property<Boolean> privateWorld() {
        return privateWorld;
    }

    @Override
    public boolean isPrivateWorld() {
        return privateWorld.get();
    }

    @Override
    public void setPrivateWorld(boolean privateWorld) {
        this.privateWorld.set(privateWorld);
    }

    @Override
    public Property<Integer> timeSinceBackup() {
        return timeSinceBackup;
    }

    @Override
    public int getTimeSinceBackup() {
        return timeSinceBackup.get();
    }

    @Override
    public void setTimeSinceBackup(int timeSinceBackup) {
        this.timeSinceBackup.set(timeSinceBackup);
    }

    @Override
    public Property<Long> lastEdited() {
        return lastEdited;
    }

    @Override
    public long getLastEdited() {
        return lastEdited.get();
    }

    @Override
    public void setLastEdited(long lastEdited) {
        this.lastEdited.set(lastEdited);
    }

    @Override
    public Property<Long> lastLoaded() {
        return lastLoaded;
    }

    @Override
    public long getLastLoaded() {
        return lastLoaded.get();
    }

    @Override
    public void setLastLoaded(long lastLoaded) {
        this.lastLoaded.set(lastLoaded);
    }

    @Override
    public Property<Long> lastUnloaded() {
        return lastUnloaded;
    }

    @Override
    public long getLastUnloaded() {
        return lastUnloaded.get();
    }

    @Override
    public void setLastUnloaded(long lastUnloaded) {
        this.lastUnloaded.set(lastUnloaded);
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Gets a map of all persistent properties for the world, keyed by their storage name. This is an internal
     * serialization hook used by the storage layer and is intentionally not part of the public {@link WorldData} API.
     *
     * @return The map of storage keys to their persistent properties
     */
    public Map<String, PersistentProperty<?>> getAllData() {
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
