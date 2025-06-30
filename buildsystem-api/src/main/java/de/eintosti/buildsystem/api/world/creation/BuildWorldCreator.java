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
package de.eintosti.buildsystem.api.world.creation;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a creator for a {@link BuildWorld}.
 *
 * @since 3.0.0
 */
@NullMarked
public interface BuildWorldCreator {

    /**
     * Sets the name of the world.
     *
     * @param name The world name
     * @return The world creator object
     */
    BuildWorldCreator setName(String name);

    /**
     * Sets the creator of the world.
     *
     * @param creator The creator, may be {@code null}
     * @return The world creator object
     */
    BuildWorldCreator setCreator(@Nullable Builder creator);

    /**
     * Sets the template which the world should be copied from.
     * <p>
     * Only used if the world type is {@link BuildWorldType#TEMPLATE}
     *
     * @param template The template name, may be {@code null} if no template is used
     * @return The creator object
     */
    BuildWorldCreator setTemplate(@Nullable String template);

    /**
     * Sets the type of the world.
     *
     * @param type The world type
     * @return The world creator object
     */
    BuildWorldCreator setType(BuildWorldType type);

    /**
     * Sets the custom {@link ChunkGenerator} of the world.
     *
     * @param customGenerator The custom chunk generator
     * @return The world creator object
     */
    BuildWorldCreator setCustomGenerator(CustomGenerator customGenerator);

    /**
     * Sets the folder in which the world should be created.
     *
     * @param folder The folder where the world should be created, may be {@code null} if not to be added to a folder
     * @return The world creator object
     */
    BuildWorldCreator setFolder(@Nullable Folder folder);

    /**
     * Sets whether the world should be private or not.
     *
     * @param privateWorld Whether the world should be private
     * @return The world creator object
     */
    BuildWorldCreator setPrivate(boolean privateWorld);

    /**
     * Sets the difficulty of the world.
     *
     * @param difficulty The difficulty
     * @return The world creator object
     */
    BuildWorldCreator setDifficulty(Difficulty difficulty);

    /**
     * Sets the creation date of the world.
     *
     * @param creationDate The creation date in milliseconds since epoch
     * @return The world creator object
     */
    BuildWorldCreator setCreationDate(long creationDate);

    /**
     * Creates and generates a new {@link BuildWorld} using the settings configured in this builder.
     * <p>
     * This process includes creating the world files, registering the world with the plugin, and notifying the player of the progress.
     *
     * @param player The player who is creating the world
     */
    void createWorld(Player player);

    /**
     * Imports an existing world directory as a new {@link BuildWorld}.
     *
     * @param player   The player who is importing the world
     * @param teleport If true, the player will be teleported to the world after the import is finished
     */
    void importWorld(Player player, boolean teleport);

    /**
     * Generates the underlying Bukkit {@link World} and applies post-generation settings. Only generates the world if the world was not created in a newer Minecraft version that
     * the server is running.
     * <p>
     * Important: This method should only be called after the world has been created and registered with the plugin.
     *
     * @return The generated {@link World}, or {@code null} if generation failed
     */
    @Nullable
    default World generateBukkitWorld() {
        return generateBukkitWorld(true);
    }

    /**
     * Generates the underlying Bukkit {@link World} and applies post-generation settings.
     * <p>
     * Important: This method should only be called after the world has been created and registered with the plugin.
     *
     * @param checkVersion If true, verify that the world's data version is compatible
     * @return The generated {@link World}, or {@code null} if generation failed
     */
    @Nullable
    World generateBukkitWorld(boolean checkVersion);
}