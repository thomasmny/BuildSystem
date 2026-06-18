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
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for adopting an existing world directory as a {@link BuildWorld}.
 *
 * <p>Obtain one from {@link de.eintosti.buildsystem.api.world.WorldService#importWorld(String)}, where the name
 * identifies the existing directory under the server's world container. Configure it, then call {@link #build()}:
 *
 * <pre>{@code
 * BuildWorld world = api.getWorldService()
 *         .importWorld("old_lobby")
 *         .creator(builder)
 *         .build();
 * }</pre>
 *
 * <p>To generate a fresh world instead, use {@link de.eintosti.buildsystem.api.world.WorldService#newWorld(String)} and
 * its {@link WorldBuilder}.
 *
 * @since 4.0.0
 */
@NullMarked
public interface WorldImporter {

    /**
     * Sets the creator (owner) recorded for the imported world.
     *
     * @param creator The creator, or {@code null}
     * @return This importer
     */
    WorldImporter creator(@Nullable Builder creator);

    /**
     * Sets the type the imported world is registered as. When omitted, the world is registered as
     * {@link BuildWorldType#IMPORTED}.
     *
     * @param type The world type
     * @return This importer
     */
    WorldImporter type(BuildWorldType type);

    /**
     * Sets the custom generator used to load the world's chunks. When omitted, the world is loaded with a void
     * generator.
     *
     * @param customGenerator The custom generator, or {@code null} for the default
     * @return This importer
     */
    WorldImporter customGenerator(@Nullable CustomGenerator customGenerator);

    /**
     * Sets the folder the imported world should be placed into.
     *
     * @param folder The folder, or {@code null} for none
     * @return This importer
     */
    WorldImporter folder(@Nullable Folder folder);

    /**
     * Sets whether the imported world should be private.
     *
     * @param privateWorld {@code true} for a private world
     * @return This importer
     */
    WorldImporter privateWorld(boolean privateWorld);

    /**
     * Registers a {@link Player} to receive failure messages (e.g. the world was created in a newer Minecraft version).
     * When omitted, {@link #build()} produces no chat output.
     *
     * @param audience The player to notify, or {@code null} for no notifications
     * @return This importer
     */
    WorldImporter notify(@Nullable Player audience);

    /**
     * Imports and registers the existing world directory using the configured settings.
     *
     * @return The imported {@link BuildWorld}, or {@code null} if the import failed (e.g. the world's data version is
     *     newer than the server). When an audience is set via {@link #notify(Player)}, the reason is sent to that
     *     player.
     * @apiNote World generation goes through Bukkit's {@code WorldCreator}, which is main-thread only. This method
     *     <b>must be called on the Bukkit main thread</b> and completes synchronously before it returns. It does not
     *     teleport anyone; teleport explicitly via {@code world.getTeleporter().teleport(player)} if desired.
     */
    @Nullable BuildWorld build();
}
