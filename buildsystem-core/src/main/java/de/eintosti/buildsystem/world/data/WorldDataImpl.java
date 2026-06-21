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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.world.data.type.Bypassable;
import de.eintosti.buildsystem.world.data.type.ConfigurableProperty;
import de.eintosti.buildsystem.world.data.type.Overridable;
import de.eintosti.buildsystem.world.data.type.PersistentProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
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

    private WorldDataImpl(WorldDataBuilder builder) {
        this.worldName = builder.worldName;

        register(WorldDataKey.CUSTOM_SPAWN, new ConfigurableProperty<>(builder.customSpawn));
        register(
                WorldDataKey.PERMISSION,
                new ConfigurableProperty<>(builder.permission)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.permission"))
                        .withCapability(
                                Overridable.class,
                                folderOverride(builder.permissionOverrideEnabled, Folder::getPermission)));
        register(
                WorldDataKey.PROJECT,
                new ConfigurableProperty<>(builder.project)
                        .withCapability(
                                Overridable.class, folderOverride(builder.projectOverrideEnabled, Folder::getProject)));

        register(
                WorldDataKey.DIFFICULTY,
                new ConfigurableProperty<>(builder.difficulty).withConfigFormatter(Difficulty::name));
        register(
                WorldDataKey.MATERIAL,
                new ConfigurableProperty<>(builder.material).withConfigFormatter(XMaterial::name));
        register(WorldDataKey.ICON_SKULL_TEXTURE, new ConfigurableProperty<>(builder.iconSkullTexture));
        register(
                WorldDataKey.STATUS,
                new ConfigurableProperty<>(Objects.requireNonNull(builder.status, "status"))
                        .withConfigFormatter(BuildWorldStatus::getId)
                        .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.archive")));

        register(WorldDataKey.BLOCK_BREAKING, settingsBypassable(builder.blockBreaking));
        register(WorldDataKey.BLOCK_INTERACTIONS, settingsBypassable(builder.blockInteractions));
        register(WorldDataKey.BLOCK_PLACEMENT, settingsBypassable(builder.blockPlacement));
        register(WorldDataKey.BUILDERS_ENABLED, new ConfigurableProperty<>(builder.buildersEnabled));
        register(WorldDataKey.EXPLOSIONS, new ConfigurableProperty<>(builder.explosions));
        register(WorldDataKey.MOB_AI, new ConfigurableProperty<>(builder.mobAi));
        register(WorldDataKey.PHYSICS, new ConfigurableProperty<>(builder.physics));
        register(WorldDataKey.PINNED, new ConfigurableProperty<>(builder.pinned));
        register(
                WorldDataKey.VISIBILITY,
                new ConfigurableProperty<>(builder.visibility).withConfigFormatter(Visibility::name));

        register(WorldDataKey.TIME_SINCE_BACKUP, new ConfigurableProperty<>(builder.timeSinceBackup));
        register(WorldDataKey.LAST_EDITED, new ConfigurableProperty<>(builder.lastEdited));
        register(WorldDataKey.LAST_LOADED, new ConfigurableProperty<>(builder.lastLoaded));
        register(WorldDataKey.LAST_UNLOADED, new ConfigurableProperty<>(builder.lastUnloaded));
    }

    public void setFolderResolver(Supplier<@Nullable Folder> resolver) {
        this.folderResolver = resolver;
    }

    @SuppressWarnings("unchecked")
    public void setStatusChangeListener(BiConsumer<BuildWorldStatus, BuildWorldStatus> listener) {
        ((ConfigurableProperty<BuildWorldStatus>) property(WorldDataKey.STATUS)).setChangeListener(listener);
    }

    private @Nullable Folder getAssignedFolder() {
        Supplier<@Nullable Folder> resolver = this.folderResolver;
        return resolver != null ? resolver.get() : null;
    }

    private void register(WorldDataKey<?> key, ConfigurableProperty<?> property) {
        this.data.put(key.id(), property);
    }

    private PersistentProperty<?> property(WorldDataKey<?> key) {
        PersistentProperty<?> property = this.data.get(key.id());
        if (property == null) {
            throw new IllegalArgumentException("Unknown world data key: " + key.id());
        }
        return property;
    }

    /**
     * Builds a boolean setting property that may be bypassed with the {@code buildsystem.bypass.settings} permission.
     */
    private static ConfigurableProperty<Boolean> settingsBypassable(boolean defaultValue) {
        return new ConfigurableProperty<>(defaultValue)
                .withCapability(Bypassable.class, new Bypassable("buildsystem.bypass.settings"));
    }

    /**
     * Builds an {@link Overridable} capability that draws its override value from this world's assigned folder, or
     * {@code null} when the world has no folder.
     */
    private Overridable<String> folderOverride(BooleanSupplier enabled, Function<Folder, @Nullable String> extractor) {
        return new Overridable<>(enabled, () -> {
            Folder folder = getAssignedFolder();
            return folder != null ? extractor.apply(folder) : null;
        });
    }

    @Override
    public <T> T get(WorldDataKey<T> key) {
        return key.type().cast(property(key).get());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void set(WorldDataKey<T> key, T value) {
        ((PersistentProperty<T>) property(key)).set(value);
    }

    @Override
    public @Nullable Location getCustomSpawnLocation() {
        return CustomSpawn.parse(Bukkit.getWorld(worldName), get(WorldDataKey.CUSTOM_SPAWN));
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Map<String, PersistentProperty<?>> getAllData() {
        return Collections.unmodifiableMap(data);
    }

    public static class WorldDataBuilder {

        private final String worldName;

        private String customSpawn = "";
        private String permission = "-";
        private String project = "-";
        private Difficulty difficulty = Difficulty.PEACEFUL;
        private XMaterial material = XMaterial.GRASS_BLOCK;
        private String iconSkullTexture = "";
        private @Nullable BuildWorldStatus status;
        private boolean blockBreaking = true;
        private boolean blockInteractions = true;
        private boolean blockPlacement = true;
        private boolean buildersEnabled = false;
        private boolean explosions = true;
        private boolean mobAi = true;
        private boolean physics = true;
        private boolean pinned = false;
        private Visibility visibility = Visibility.EVERYONE;
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
         * Builds the {@link WorldDataImpl} instance from this builder's configured values.
         *
         * @return A new, fully-initialized {@link WorldDataImpl}. The returned instance is mutable — its properties can
         *     still be changed afterwards through the {@code set*} methods.
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

        public WorldDataBuilder withIconSkullTexture(String iconSkullTexture) {
            this.iconSkullTexture = iconSkullTexture;
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

        public WorldDataBuilder withPinned(boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        public WorldDataBuilder withVisibility(Visibility visibility) {
            this.visibility = visibility;
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
