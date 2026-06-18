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
     * @since 4.0.0
     */
    String getCustomSpawn();

    /**
     * Sets the custom spawn of the {@link BuildWorld} as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @param customSpawn The custom spawn string
     * @since 4.0.0
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
     * @since 4.0.0
     */
    String getPermission();

    /**
     * Sets the permission required to enter the {@link BuildWorld}.
     *
     * @param permission The permission string
     * @since 4.0.0
     */
    void setPermission(String permission);

    /**
     * Gets the project description of the {@link BuildWorld}.
     *
     * @return The project description string
     * @since 4.0.0
     */
    String getProject();

    /**
     * Sets the project description of the {@link BuildWorld}.
     *
     * @param project The project description string
     * @since 4.0.0
     */
    void setProject(String project);

    /**
     * Gets the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @return The world's difficulty setting
     * @since 4.0.0
     */
    Difficulty getDifficulty();

    /**
     * Sets the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @param difficulty The world's difficulty setting
     * @since 4.0.0
     */
    void setDifficulty(Difficulty difficulty);

    /**
     * Gets the {@link XMaterial} used to display the {@link BuildWorld} in the navigator menus.
     *
     * @return The material used for display
     * @since 4.0.0
     */
    XMaterial getMaterial();

    /**
     * Sets the {@link XMaterial} used to display the {@link BuildWorld} in the navigator menus.
     *
     * @param material The material used for display
     * @since 4.0.0
     */
    void setMaterial(XMaterial material);

    /**
     * Gets the skull texture applied when the world's {@link #getMaterial() icon} is a player head. Returns {@code null}
     * when none is set; the literal {@code "%viewer%"} means the viewing player's own head.
     *
     * @return The skull texture, {@code "%viewer%"}, or {@code null}
     * @since 4.0.0
     */
    @Nullable String getIconSkullTexture();

    /**
     * Sets the skull texture applied when the world's {@link #getMaterial() icon} is a player head. Pass {@code null} to
     * clear it, or the literal {@code "%viewer%"} to render the viewing player's own head.
     *
     * @param skullTexture The skull texture, {@code "%viewer%"}, or {@code null}
     * @since 4.0.0
     */
    void setIconSkullTexture(@Nullable String skullTexture);

    /**
     * Gets the current {@link BuildWorldStatus} of the {@link BuildWorld}.
     *
     * @return The current build status
     * @since 4.0.0
     */
    BuildWorldStatus getStatus();

    /**
     * Sets the current {@link BuildWorldStatus} of the {@link BuildWorld}.
     *
     * @param status The current build status
     * @since 4.0.0
     */
    void setStatus(BuildWorldStatus status);

    /**
     * Gets whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @return {@code true} if allowed, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isBlockBreaking();

    /**
     * Sets whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @param blockBreaking {@code true} if allowed, otherwise {@code false}
     * @since 4.0.0
     */
    void setBlockBreaking(boolean blockBreaking);

    /**
     * Gets whether block interactions (e.g., opening doors, chests) are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isBlockInteractions();

    /**
     * Sets whether block interactions are enabled in the {@link BuildWorld}.
     *
     * @param blockInteractions {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    void setBlockInteractions(boolean blockInteractions);

    /**
     * Gets whether block placement is allowed in the {@link BuildWorld}.
     *
     * @return {@code true} if allowed, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isBlockPlacement();

    /**
     * Sets whether block placement is allowed in the {@link BuildWorld}.
     *
     * @param blockPlacement {@code true} if allowed, otherwise {@code false}
     * @since 4.0.0
     */
    void setBlockPlacement(boolean blockPlacement);

    /**
     * Gets whether the "builders feature" is enabled in the {@link BuildWorld}. When enabled, only designated builders
     * can modify the world.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isBuildersEnabled();

    /**
     * Sets whether the "builders feature" is enabled in the {@link BuildWorld}.
     *
     * @param buildersEnabled {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    void setBuildersEnabled(boolean buildersEnabled);

    /**
     * Gets whether explosions are enabled in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isExplosions();

    /**
     * Sets whether explosions are enabled in the {@link BuildWorld}.
     *
     * @param explosions {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    void setExplosions(boolean explosions);

    /**
     * Gets whether entities in the {@link BuildWorld} have artificial intelligence.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isMobAi();

    /**
     * Sets whether entities in the {@link BuildWorld} have artificial intelligence.
     *
     * @param mobAi {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    void setMobAi(boolean mobAi);

    /**
     * Gets whether physics (e.g., gravity, fluid flow) is applied to blocks in the {@link BuildWorld}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isPhysics();

    /**
     * Sets whether physics is applied to blocks in the {@link BuildWorld}.
     *
     * @param physics {@code true} if enabled, otherwise {@code false}
     * @since 4.0.0
     */
    void setPhysics(boolean physics);

    /**
     * Gets whether this world is pinned to the top of the navigator, ahead of unpinned worlds regardless of the active
     * sort order.
     *
     * @return {@code true} if pinned, otherwise {@code false}
     * @since 4.0.0
     */
    boolean isPinned();

    /**
     * Sets whether this world is pinned to the top of the navigator.
     *
     * @param pinned {@code true} if pinned, otherwise {@code false}
     * @since 4.0.0
     */
    void setPinned(boolean pinned);

    /**
     * Gets the {@link Visibility} governing who may see and enter the {@link BuildWorld}. This is the source of truth
     * for a world's visibility, replacing the legacy private boolean.
     *
     * @return The access rule
     * @since 4.0.0
     */
    Visibility getVisibility();

    /**
     * Sets the {@link Visibility} governing who may see and enter the {@link BuildWorld}.
     *
     * @param visibility The access rule
     * @since 4.0.0
     */
    void setVisibility(Visibility visibility);

    /**
     * Gets the number of seconds that have passed since the last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @return The number of seconds since the last backup
     * @since 4.0.0
     */
    int getTimeSinceBackup();

    /**
     * Sets the number of seconds that have passed since the last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @param timeSinceBackup The number of seconds since the last backup
     * @since 4.0.0
     */
    void setTimeSinceBackup(int timeSinceBackup);

    /**
     * Gets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was edited.
     *
     * @return The last edited timestamp
     * @since 4.0.0
     */
    long getLastEdited();

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was edited.
     *
     * @param lastEdited The last edited timestamp
     * @since 4.0.0
     */
    void setLastEdited(long lastEdited);

    /**
     * Gets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was loaded.
     *
     * @return The last loaded timestamp
     * @since 4.0.0
     */
    long getLastLoaded();

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was loaded.
     *
     * @param lastLoaded The last loaded timestamp
     * @since 4.0.0
     */
    void setLastLoaded(long lastLoaded);

    /**
     * Gets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was unloaded.
     *
     * @return The last unloaded timestamp
     * @since 4.0.0
     */
    long getLastUnloaded();

    /**
     * Sets the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was unloaded.
     *
     * @param lastUnloaded The last unloaded timestamp
     * @since 4.0.0
     */
    void setLastUnloaded(long lastUnloaded);
}
