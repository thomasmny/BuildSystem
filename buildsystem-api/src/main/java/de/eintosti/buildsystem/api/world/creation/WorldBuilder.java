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
package de.eintosti.buildsystem.api.world.creation;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import org.bukkit.Difficulty;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for generating a brand-new {@link BuildWorld}.
 *
 * <p>Obtain one from {@link de.eintosti.buildsystem.api.world.WorldService#newWorld(String)}, configure it, then call
 * {@link #build()} to generate and register the world:
 *
 * <pre>{@code
 * BuildWorld world = api.getWorldService()
 *         .newWorld("Lobby")
 *         .type(BuildWorldType.NORMAL)
 *         .difficulty(Difficulty.PEACEFUL)
 *         .creator(builder)
 *         .build();
 * }</pre>
 *
 * <p>To adopt an existing world directory instead of generating a new one, use
 * {@link de.eintosti.buildsystem.api.world.WorldService#importWorld(String)} and its {@link WorldImporter}.
 *
 * @since 3.1.0
 */
@NullMarked
public interface WorldBuilder {

    /**
     * Sets the type of the world. Defaults to {@link BuildWorldType#NORMAL}.
     *
     * @param type The world type
     * @return This builder
     */
    WorldBuilder type(BuildWorldType type);

    /**
     * Sets the template the world should be copied from. Only applied when the type is {@link BuildWorldType#TEMPLATE}.
     *
     * @param template The template name, or {@code null} for none
     * @return This builder
     */
    WorldBuilder template(@Nullable String template);

    /**
     * Sets the custom generator used to generate the world's chunks.
     *
     * @param customGenerator The custom generator, or {@code null} for none
     * @return This builder
     */
    WorldBuilder customGenerator(@Nullable CustomGenerator customGenerator);

    /**
     * Sets the difficulty of the world.
     *
     * @param difficulty The difficulty
     * @return This builder
     */
    WorldBuilder difficulty(Difficulty difficulty);

    /**
     * Sets the creator (owner) of the world. When {@code null} the world has no recorded creator.
     *
     * @param creator The creator, or {@code null}
     * @return This builder
     */
    WorldBuilder creator(@Nullable Builder creator);

    /**
     * Sets the folder the world should be placed into.
     *
     * @param folder The folder, or {@code null} for none
     * @return This builder
     */
    WorldBuilder folder(@Nullable Folder folder);

    /**
     * Sets whether the world should be private.
     *
     * @param privateWorld {@code true} for a private world
     * @return This builder
     */
    WorldBuilder privateWorld(boolean privateWorld);

    /**
     * Sets the creation date of the world, in milliseconds since the epoch.
     *
     * @param creationDate The creation timestamp
     * @return This builder
     */
    WorldBuilder creationDate(long creationDate);

    /**
     * Registers a {@link Player} to receive progress messages (creation started/finished, and failure reasons such as a
     * name already in use). When omitted, {@link #build()} produces no chat output — appropriate for headless callers.
     *
     * @param audience The player to notify, or {@code null} for no notifications
     * @return This builder
     */
    WorldBuilder notify(@Nullable Player audience);

    /**
     * Generates and registers the world using the configured settings.
     *
     * @return The created {@link BuildWorld}, or {@code null} if creation failed (e.g. the name is already in use or a
     *     referenced template does not exist). When an audience is set via {@link #notify(Player)}, the reason is sent
     *     to that player.
     * @apiNote World generation goes through Bukkit's {@code WorldCreator}, which is main-thread only. This method
     *     <b>must be called on the Bukkit main thread</b> and completes synchronously before it returns. It does not
     *     teleport anyone; teleport explicitly via {@code world.getTeleporter().teleport(player)} if desired.
     */
    @Nullable BuildWorld build();
}
