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
import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import java.util.Map;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manages and provides access to various data points and settings for a {@link BuildWorld}. This interface allows for reading and modifying world-specific configurations.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldData {

    /**
     * Retrieves a {@link Type} object representing the custom spawn location of the {@link BuildWorld}. The value is stored as a string in the format {@code x;y;z;yaw;pitch}.
     *
     * @return A {@link Type} containing the custom spawn string
     * @see #getCustomSpawnLocation()
     */
    Type<String> customSpawn();

    /**
     * Gets the {@link BuildWorld}'s custom spawn as a {@link Location} object.
     *
     * @return The custom spawn as a location, or {@code null} if not set or invalid
     */
    @Nullable
    Location getCustomSpawnLocation();

    /**
     * Retrieves a {@link Type} object representing the permission required to enter the {@link BuildWorld}. Returns "-" if no specific permission is required.
     *
     * @return A {@link Type} containing the permission string
     */
    Type<String> permission();

    /**
     * Retrieves a {@link Type} object representing the project description of the {@link BuildWorld}. This typically provides a brief overview or purpose of the world.
     *
     * @return A {@link Type} containing the project description string
     */
    Type<String> project();

    /**
     * Retrieves a {@link Type} object representing the {@link Difficulty} of the {@link BuildWorld}.
     *
     * @return A {@link Type} containing the world's difficulty setting
     */
    Type<Difficulty> difficulty();

    /**
     * Retrieves a {@link Type} object representing the {@link XMaterial} used to display the {@link BuildWorld} in the navigator menus.
     *
     * @return A {@link Type} containing the material used for display
     */
    Type<XMaterial> material();

    /**
     * Retrieves a {@link Type} object representing the current {@link BuildWorldStatus} of the world. This indicates the building progression or state of the world.
     *
     * @return A {@link Type} containing the current build status
     */
    Type<BuildWorldStatus> status();

    /**
     * Retrieves a {@link Type} object indicating whether block breaking is allowed in the {@link BuildWorld}.
     *
     * @return A {@link Type} containing a boolean: {@code true} if allowed, otherwise {@code false}
     */
    Type<Boolean> blockBreaking();

    /**
     * Retrieves a {@link Type} object indicating whether block interactions (e.g., opening doors, chests) are enabled in the {@link BuildWorld}.
     *
     * @return A {@link Type} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> blockInteractions();

    /**
     * Retrieves a {@link Type} object indicating whether block placement is allowed in the {@link BuildWorld}.
     *
     * @return A {@link Type} containing a boolean: {@code true} if allowed, otherwise {@code false}
     */
    Type<Boolean> blockPlacement();

    /**
     * Retrieves a {@link Type} object indicating whether the "builders feature" is enabled in the {@link BuildWorld}. When enabled, only designated builders can modify the world.
     *
     * @return A {@link Type} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> buildersEnabled();

    /**
     * Retrieves a {@link Type} object indicating whether explosions are enabled in the {@link BuildWorld}.
     *
     * @return A {@link Type} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> explosions();

    /**
     * Retrieves a {@link Type} object indicating whether entities in the {@link BuildWorld} have artificial intelligence.
     *
     * @return A {@link Type} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> mobAi();

    /**
     * Retrieves a {@link Type} object indicating whether physics (e.g., gravity, fluid flow) is applied to blocks in the {@link BuildWorld}.
     *
     * @return A {@link Type} containing a boolean: {@code true} if enabled, otherwise {@code false}
     */
    Type<Boolean> physics();

    /**
     * Retrieves a {@link Type} object indicating whether the {@link BuildWorld} is set to private visibility. A private world is typically only accessible to its creator and
     * designated builders.
     *
     * @return A {@link Type} containing a boolean: {@code true} if private, otherwise {@code false}
     */
    Type<Boolean> privateWorld();

    /**
     * Gets the number of seconds that have passed since that last {@link Backup} of the {@link BuildWorld} was created.
     *
     * @return The number of seconds since the last backup
     */
    Type<Integer> timeSinceBackup();

    /**
     * Retrieves a {@link Type} object representing the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was edited.
     *
     * @return A {@link Type} containing the last edited timestamp
     */
    Type<Long> lastEdited();

    /**
     * Retrieves a {@link Type} object representing the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was loaded.
     *
     * @return A {@link Type} containing the last loaded timestamp
     */
    Type<Long> lastLoaded();

    /**
     * Retrieves a {@link Type} object representing the timestamp (in milliseconds since epoch) of the last time the {@link BuildWorld} was unloaded.
     *
     * @return A {@link Type} containing the last unloaded timestamp
     */
    Type<Long> lastUnloaded();

    /**
     * Gets a map of all configurable data points for the {@link BuildWorld}.
     *
     * @return An unmodifiable map where keys are data point names and values are their corresponding {@link Type} objects
     */
    Map<String, Type<?>> getAllData();
}