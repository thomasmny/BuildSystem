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
     * Gets the custom spawn of the {@link BuildWorld} as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @return The custom spawn string
     * @since TODO
     */
    String getCustomSpawn();

    /**
     * Sets the custom spawn of the {@link BuildWorld} as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @param customSpawn The custom spawn string
     * @since TODO
     */
    void setCustomSpawn(String customSpawn);

    /**
     * Gets the {@link BuildWorld}'s custom spawn as a {@link Location} object.
     *
     * @return The custom spawn as a location, or {@code null} if not set or invalid
     */
    @Nullable Location getCustomSpawnLocation();

    /**
     * Gets the permission required to enter the {@link BuildWorld}. Returns "-" if no specific permission is required.
     *
     * @return The permission string
     * @since TODO
     */
    String getPermission();

    /**
     * Sets the permission required to enter the {@link BuildWorld}.
     *
     * @param permission The permission string
     * @since TODO
     */
    void setPermission(String permission);

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
     * Gets the current {@link BuildWorldStatus} of the {@link BuildWorld}.
     *
     * @return The current build status
     * @since TODO
     */
    BuildWorldStatus getStatus();

    /**
     * Sets the current {@link BuildWorldStatus} of the {@link BuildWorld}.
     *
     * @param status The current build status
     * @since TODO
     */
    void setStatus(BuildWorldStatus status);

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
     * Gets whether block interactions (e.g., opening doors, chests) are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    boolean isBlockInteractions();

    /**
     * Sets whether block interactions are enabled in the {@link BuildWorld}.
     *
     * @param blockInteractions {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setBlockInteractions(boolean blockInteractions);

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
     * Gets whether the "builders feature" is enabled in the {@link BuildWorld}. When enabled, only designated builders
     * can modify the world.
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
     * Gets whether physics (e.g., gravity, fluid flow) is applied to blocks in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    boolean isPhysics();

    /**
     * Sets whether physics is applied to blocks in the {@link BuildWorld}.
     *
     * @param physics {@code true} if enabled, otherwise {@code false}
     * @since TODO
     */
    void setPhysics(boolean physics);

    /**
     * Gets whether this world is pinned to the top of the navigator, ahead of unpinned worlds regardless of the active
     * sort order.
     *
     * @return {@code true} if pinned, otherwise {@code false}
     * @since TODO
     */
    boolean isPinned();

    /**
     * Sets whether this world is pinned to the top of the navigator.
     *
     * @param pinned {@code true} if pinned, otherwise {@code false}
     * @since TODO
     */
    void setPinned(boolean pinned);

    /**
     * Gets whether the {@link BuildWorld} is set to private visibility. A private world is typically only accessible to
     * its creator and designated builders.
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
