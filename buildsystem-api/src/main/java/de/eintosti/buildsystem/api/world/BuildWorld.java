/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.api.world;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.util.WorldLoader;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.api.world.util.WorldTeleporter;
import de.eintosti.buildsystem.api.world.util.WorldUnloader;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public interface BuildWorld extends Displayable {

    /**
     * Gets the Bukkit {@link World} associated with this {@link BuildWorld}.
     *
     * @return The Bukkit world, or {@code null} if not loaded
     */
    @Nullable
    World getWorld();

    /**
     * Gets the unique identifier of the world.
     * <p>
     * Not equivalent to {@link World#getUID()}.
     *
     * @return The uuid of the world
     */
    UUID getUniqueId();

    /**
     * Sets the name of the world.
     *
     * @param name The name to set to
     */
    void setName(String name);

    Profileable asProfilable();

    /**
     * Gets the world's type.
     *
     * @return The {@link BuildWorldType} of the world
     */
    BuildWorldType getType();

    /**
     * Gets the world's data.
     *
     * @return The {@link WorldData} of the world
     */
    WorldData getData();

    /**
     * Gets the creation date of the world.
     *
     * @return The number of milliseconds that have passed since {@code January 1, 1970 UTC}, until the world was created.
     */
    long getCreationDate();

    /**
     * Gets the custom chunk generator used to generate the world.
     *
     * @return The custom chunk generator used to generate the world.
     */
    @Nullable
    CustomGenerator getCustomGenerator();

    /**
     * Cycles to the next {@link Difficulty}.
     */
    void cycleDifficulty();

    /**
     * Gets a list of all builders who can modify the world.
     *
     * @return the list of all builders
     */
    Builders getBuilders();

    /**
     * Gets the time in the {@link World} linked to the build world.
     *
     * @return The world time
     */
    String getWorldTime();

    /**
     * Gets whether the world is currently loaded, allowing a player to enter it.
     *
     * @return {@code true} if the world is loaded, otherwise {@code false}
     */
    boolean isLoaded();

    /**
     * Sets whether the world is currently loaded, allowing a player to enter it.
     *
     * @param loaded {@code true} if the world is loaded, otherwise {@code false}
     */
    void setLoaded(boolean loaded);

    /**
     * Gets the {@link WorldLoader} used to load the world.
     *
     * @return The {@link WorldLoader} used to load the world
     */
    WorldLoader getLoader();

    /**
     * Gets the {@link WorldUnloader} used to unload the world.
     *
     * @return The {@link WorldUnloader} used to unload the world
     */
    WorldUnloader getUnloader();

    /**
     * Gets the {@link WorldTeleporter} used to teleport players to the world.
     *
     * @return The {@link WorldTeleporter} used to teleport players to the world
     */
    WorldTeleporter getTeleporter();

    /**
     * Gets the {@link WorldPermissions} associated with the world.
     *
     * @return The {@link WorldPermissions} for the world
     */
    WorldPermissions getPermissions();
}