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
package de.eintosti.buildsystem.api.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.data.Property;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manages and provides access to various data points and settings for a {@link BuildWorld}. This interface allows for
 * reading and modifying world-specific configurations.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldData {

    /**
     * Retrieves a {@link Property} object representing the custom spawn location of the {@link BuildWorld}. The value is
     * stored as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @return A {@link Property} containing the custom spawn string
     * @see #getCustomSpawnLocation()
     */
    Property<String> customSpawn();

    /**
     * Gets the custom spawn of the {@link BuildWorld} as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @return The custom spawn string
     * @since TODO
     */
    default String getCustomSpawn() {
        return customSpawn().get();
    }

    /**
     * Sets the custom spawn of the {@link BuildWorld} as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @param customSpawn The custom spawn string
     * @since TODO
     */
    default void setCustomSpawn(String customSpawn) {
        customSpawn().set(customSpawn);
    }

    /**
     * Gets the {@link BuildWorld}'s custom spawn as a {@link Location} object.
     *
     * @return The custom spawn as a location, or {@code null} if not set or invalid
     */
    @Nullable Location getCustomSpawnLocation();

    /**
     * Retrieves a {@link Property} object representing the permission required to enter the {@link BuildWorld}. Returns
     * "-" if no specific permission is required.
     *
     * @return A {@link Property} containing the permission string
     */
    Property<String> permission();

    /**
     * Gets the permission required to enter the {@link BuildWorld}.
     *
     * @return The permission string
     * @since TODO
     */
    default String getPermission() {
        return permission().get();
    }

    /**
     * Sets the permission required to enter the {@link BuildWorld}.
     *
     * @param permission The permission string
     * @since TODO
     */
    default void setPermission(String permission) {
        permission().set(permission);
    }

    /**
     * Retrieves a {@link Property} object representing the project description of the {@link BuildWorld}. This typically
     * provides a brief overview or purpose of the world.
     *
     * @return A {@link Property} containing the project description string
     */
    Property<String> project();

    /**
     * Gets the project description of the {@link BuildWorld}.
     *
     * @return The project description string
     * @since TODO
     */
    default String getProject() {
        return project().get();
    }

    /**
     * Sets the project description of the {@link BuildWorld}.
     *
     * @param project The project description string
     * @since TODO
     */
    default void setProject(String project) {
        project().set(project);
    }

    /**
     * Retrieves a {@link Property} object representing the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @return A {@link Property} containing the world's difficulty setting
     */
    Property<Difficulty> difficulty();

    /**
     * Gets the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @return The world's difficulty setting
     * @since TODO
     */
    default Difficulty getDifficulty() {
        return difficulty().get();
    }

    /**
     * Sets the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @param difficulty The world's difficulty setting
     * @since TODO
     */
    default void setDifficulty(Difficulty difficulty) {
        difficulty().set(difficulty);
    }

    /**
     * Retrieves a {@link Property} object representing the {@link XMaterial} used to display the {@link BuildWorld} in
     * the navigator menus.
     *
     * @return A {@link Property} containing the material used for display
     */
    Property<XMaterial> material();

    /**
     * Gets the {@link XMaterial} used to display the {@link BuildWorld} in the navigator menus.
     *
     * @return The material used for display
     * @since TODO
     */
    default XMaterial getMaterial() {
        return material().get();
    }

    /**
     * Sets the {@link XMaterial} used to display the {@link BuildWorld} in the navigator menus.
     *
     * @param material The material used for display
     * @since TODO
     */
    default void setMaterial(XMaterial material) {
        material().set(material);
    }

    /**
     * Retrieves a {@link Property} object representing the current {@link BuildWorldStatus} of the world. This indicates
     * the building progression or state of the world.
     *
     * @return A {@link Property} containing the current build status
     */
    Property<BuildWorldStatus> status();

    /**
     * Gets the current {@link BuildWorldStatus} of the {@link BuildWorld}.
     *
     * @return The current build status
     * @since TODO
     */
    default BuildWorldStatus getStatus() {
        return status().get();
    }

    /**
     * Sets the current {@link BuildWorldStatus} of the {@link BuildWorld}.
     *
     * @param status The current build status
     * @since TODO
     */
    default void setStatus(BuildWorldStatus status) {
        status().set(status);
    }

    /**
     * Retrieves a {@link Property} object indicating whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if allowed, otherwise {@code false}
     */
    Property<Boolean> blockBreaking();

    /**
     * Gets whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @return {@code true} if allowed, otherwise {@code false}
     * @since TODO
     */
    default boolean isBlockBreaking() {
        return blockBreaking().get();
    }

    /**
     * Sets whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @param blockBreaking {@code true} if allowed, otherwise {@code false}
     * @since TODO
     */
    default void setBlockBreaking(boolean blockBreaking) {
        blockBreaking().set(blockBreaking);
    }

    /**
     * Retrieves a {@link Property} object indicating whether block interactions (e.g., opening doors, chests) are
     * enabled in the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> blockInteractions();

    /**
     * Gets whether block interactions are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default boolean isBlockInteractions() {
        return blockInteractions().get();
    }

    /**
     * Sets whether block interactions are enabled in the {@link BuildWorld}.
     *
     * @param blockInteractions {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default void setBlockInteractions(boolean blockInteractions) {
        blockInteractions().set(blockInteractions);
    }

    /**
     * Retrieves a {@link Property} object indicating whether block placement is allowed in the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if allowed, otherwise {@code false}
     */
    Property<Boolean> blockPlacement();

    /**
     * Gets whether block placement is allowed in the {@link BuildWorld}.
     *
     * @return {@code true} if allowed, otherwise {@code false}
     * @since TODO
     */
    default boolean isBlockPlacement() {
        return blockPlacement().get();
    }

    /**
     * Sets whether block placement is allowed in the {@link BuildWorld}.
     *
     * @param blockPlacement {@code true} if allowed, otherwise {@code false}
     * @since TODO
     */
    default void setBlockPlacement(boolean blockPlacement) {
        blockPlacement().set(blockPlacement);
    }

    /**
     * Retrieves a {@link Property} object indicating whether the "builders feature" is enabled in the
     * {@link BuildWorld}. When enabled, only designated builders can modify the world.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> buildersEnabled();

    /**
     * Gets whether the "builders feature" is enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default boolean isBuildersEnabled() {
        return buildersEnabled().get();
    }

    /**
     * Sets whether the "builders feature" is enabled in the {@link BuildWorld}.
     *
     * @param buildersEnabled {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default void setBuildersEnabled(boolean buildersEnabled) {
        buildersEnabled().set(buildersEnabled);
    }

    /**
     * Retrieves a {@link Property} object indicating whether explosions are enabled in the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> explosions();

    /**
     * Gets whether explosions are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default boolean isExplosions() {
        return explosions().get();
    }

    /**
     * Sets whether explosions are enabled in the {@link BuildWorld}.
     *
     * @param explosions {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default void setExplosions(boolean explosions) {
        explosions().set(explosions);
    }

    /**
     * Retrieves a {@link Property} object indicating whether entities in the {@link BuildWorld} have artificial
     * intelligence.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> mobAi();

    /**
     * Gets whether entities in the {@link BuildWorld} have artificial intelligence.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default boolean isMobAi() {
        return mobAi().get();
    }

    /**
     * Sets whether entities in the {@link BuildWorld} have artificial intelligence.
     *
     * @param mobAi {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default void setMobAi(boolean mobAi) {
        mobAi().set(mobAi);
    }

    /**
     * Retrieves a {@link Property} object indicating whether physics (e.g., gravity, fluid flow) is applied to blocks in
     * the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> physics();

    /**
     * Gets whether physics is applied to blocks in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default boolean isPhysics() {
        return physics().get();
    }

    /**
     * Sets whether physics is applied to blocks in the {@link BuildWorld}.
     *
     * @param physics {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    default void setPhysics(boolean physics) {
        physics().set(physics);
    }

    /**
     * Retrieves a {@link Property} object indicating whether this world is pinned to the top of the navigator, ahead of
     * unpinned worlds regardless of the active sort order.
     *
     * @return A {@link Property} containing a boolean: {@code true} if pinned, otherwise {@code false}
     * @since TODO
     */
    Property<Boolean> pinned();

    /**
     * Gets whether this world is pinned to the top of the navigator.
     *
     * @return {@code true} if pinned, otherwise {@code false}
     * @since TODO
     */
    default boolean isPinned() {
        return pinned().get();
    }

    /**
     * Sets whether this world is pinned to the top of the navigator.
     *
     * @param pinned {@code true} if pinned, otherwise {@code false}
     * @since TODO
     */
    default void setPinned(boolean pinned) {
        pinned().set(pinned);
    }

    /**
     * Retrieves a {@link Property} object indicating whether the {@link BuildWorld} is set to private visibility. A
     * private world is typically only accessible to its creator and designated builders.
     *
     * @return A {@link Property} containing a boolean: {@code true} if private, otherwise {@code false}
     */
    Property<Boolean> privateWorld();

    /**
     * Gets whether the {@link BuildWorld} is set to private visibility.
     *
     * @return {@code true} if private, otherwise {@code false}
     * @since TODO
     */
    default boolean isPrivateWorld() {
        return privateWorld().get();
    }

    /**
     * Sets whether the {@link BuildWorld} is set to private visibility.
     *
     * @param privateWorld {@code true} if private, otherwise {@code false}
     * @since TODO
     */
    default void setPrivateWorld(boolean privateWorld) {
        privateWorld().set(privateWorld);
    }

    /**
     * Gets the number of seconds that have passed since that last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @return The number of seconds since the last backup
     */
    Property<Integer> timeSinceBackup();

    /**
     * Gets the number of seconds that have passed since the last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @return The number of seconds since the last backup
     * @since TODO
     */
    default int getTimeSinceBackup() {
        return timeSinceBackup().get();
    }

    /**
     * Sets the number of seconds that have passed since the last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @param timeSinceBackup The number of seconds since the last backup
     * @since TODO
     */
    default void setTimeSinceBackup(int timeSinceBackup) {
        timeSinceBackup().set(timeSinceBackup);
    }

    /**
     * Retrieves a {@link Property} object representing the timestamp (in milliseconds since epoch) of the last time the
     * {@link BuildWorld} was edited.
     *
     * @return A {@link Property} containing the last edited timestamp
     */
    Property<Long> lastEdited();

    /**
     * Gets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was edited.
     *
     * @return The last edited timestamp
     * @since TODO
     */
    default long getLastEdited() {
        return lastEdited().get();
    }

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was edited.
     *
     * @param lastEdited The last edited timestamp
     * @since TODO
     */
    default void setLastEdited(long lastEdited) {
        lastEdited().set(lastEdited);
    }

    /**
     * Retrieves a {@link Property} object representing the timestamp (in milliseconds since epoch) of the last time the
     * {@link BuildWorld} was loaded.
     *
     * @return A {@link Property} containing the last loaded timestamp
     */
    Property<Long> lastLoaded();

    /**
     * Gets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was loaded.
     *
     * @return The last loaded timestamp
     * @since TODO
     */
    default long getLastLoaded() {
        return lastLoaded().get();
    }

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was loaded.
     *
     * @param lastLoaded The last loaded timestamp
     * @since TODO
     */
    default void setLastLoaded(long lastLoaded) {
        lastLoaded().set(lastLoaded);
    }

    /**
     * Retrieves a {@link Property} object representing the timestamp (in milliseconds since epoch) of the last time the
     * {@link BuildWorld} was unloaded.
     *
     * @return A {@link Property} containing the last unloaded timestamp
     */
    Property<Long> lastUnloaded();

    /**
     * Gets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was unloaded.
     *
     * @return The last unloaded timestamp
     * @since TODO
     */
    default long getLastUnloaded() {
        return lastUnloaded().get();
    }

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was unloaded.
     *
     * @param lastUnloaded The last unloaded timestamp
     * @since TODO
     */
    default void setLastUnloaded(long lastUnloaded) {
        lastUnloaded().set(lastUnloaded);
    }
}
