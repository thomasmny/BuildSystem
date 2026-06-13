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
 * Manages and provides access to various properties and settings for a {@link BuildWorld}. This interface allows for
 * reading and modifying world-specific configurations.
 *
 * <p>For the common case, prefer the flat accessors (e.g. {@link #getStatus()} / {@link #setStatus(BuildWorldStatus)}).
 * The {@link Property} objects (e.g. {@link #status()}) remain available for advanced capability use.
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
     * @return The permission string, or "-" if no specific permission is required
     * @since TODO
     */
    String getPermission();

    /**
     * Sets the permission required to enter the {@link BuildWorld}.
     *
     * @param permission The permission string, or "-" if no specific permission is required
     * @since TODO
     */
    void setPermission(String permission);

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
    String getProject();

    /**
     * Sets the project description of the {@link BuildWorld}.
     *
     * @param project The project description string
     * @since TODO
     */
    void setProject(String project);

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
    Difficulty getDifficulty();

    /**
     * Sets the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @param difficulty The world's difficulty setting
     * @since TODO
     */
    void setDifficulty(Difficulty difficulty);

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
    XMaterial getMaterial();

    /**
     * Sets the {@link XMaterial} used to display the {@link BuildWorld} in the navigator menus.
     *
     * @param material The material used for display
     * @since TODO
     */
    void setMaterial(XMaterial material);

    /**
     * Retrieves a {@link Property} object representing the current {@link BuildWorldStatus} of the world. This indicates
     * the building progression or state of the world.
     *
     * @return A {@link Property} containing the current build status
     */
    Property<BuildWorldStatus> status();

    /**
     * Gets the current {@link BuildWorldStatus} of the world.
     *
     * @return The current build status
     * @since TODO
     */
    BuildWorldStatus getStatus();

    /**
     * Sets the current {@link BuildWorldStatus} of the world.
     *
     * @param status The current build status
     * @since TODO
     */
    void setStatus(BuildWorldStatus status);

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
    boolean isBlockBreaking();

    /**
     * Sets whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @param blockBreaking {@code true} if allowed, otherwise {@code false}
     * @since TODO
     */
    void setBlockBreaking(boolean blockBreaking);

    /**
     * Retrieves a {@link Property} object indicating whether block interactions (e.g., opening doors, chests) are
     * enabled in the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> blockInteractions();

    /**
     * Gets whether block interactions (e.g., opening doors, chests) are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    boolean isBlockInteractions();

    /**
     * Sets whether block interactions (e.g., opening doors, chests) are enabled in the {@link BuildWorld}.
     *
     * @param blockInteractions {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setBlockInteractions(boolean blockInteractions);

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
    boolean isBlockPlacement();

    /**
     * Sets whether block placement is allowed in the {@link BuildWorld}.
     *
     * @param blockPlacement {@code true} if allowed, otherwise {@code false}
     * @since TODO
     */
    void setBlockPlacement(boolean blockPlacement);

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
    boolean isBuildersEnabled();

    /**
     * Sets whether the "builders feature" is enabled in the {@link BuildWorld}.
     *
     * @param buildersEnabled {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setBuildersEnabled(boolean buildersEnabled);

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
    boolean isExplosions();

    /**
     * Sets whether explosions are enabled in the {@link BuildWorld}.
     *
     * @param explosions {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setExplosions(boolean explosions);

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
    boolean isMobAi();

    /**
     * Sets whether entities in the {@link BuildWorld} have artificial intelligence.
     *
     * @param mobAi {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setMobAi(boolean mobAi);

    /**
     * Retrieves a {@link Property} object indicating whether physics (e.g., gravity, fluid flow) is applied to blocks in
     * the {@link BuildWorld}.
     *
     * @return A {@link Property} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Property<Boolean> physics();

    /**
     * Gets whether physics (e.g., gravity, fluid flow) is applied to blocks in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    boolean isPhysics();

    /**
     * Sets whether physics (e.g., gravity, fluid flow) is applied to blocks in the {@link BuildWorld}.
     *
     * @param physics {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setPhysics(boolean physics);

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
    boolean isPrivateWorld();

    /**
     * Sets whether the {@link BuildWorld} is set to private visibility.
     *
     * @param privateWorld {@code true} if private, otherwise {@code false}
     * @since TODO
     */
    void setPrivateWorld(boolean privateWorld);

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
    int getTimeSinceBackup();

    /**
     * Sets the number of seconds that have passed since the last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @param timeSinceBackup The number of seconds since the last backup
     * @since TODO
     */
    void setTimeSinceBackup(int timeSinceBackup);

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
    long getLastEdited();

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was edited.
     *
     * @param lastEdited The last edited timestamp
     * @since TODO
     */
    void setLastEdited(long lastEdited);

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
    long getLastLoaded();

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was loaded.
     *
     * @param lastLoaded The last loaded timestamp
     * @since TODO
     */
    void setLastLoaded(long lastLoaded);

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
    long getLastUnloaded();

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was unloaded.
     *
     * @param lastUnloaded The last unloaded timestamp
     * @since TODO
     */
    void setLastUnloaded(long lastUnloaded);
}
