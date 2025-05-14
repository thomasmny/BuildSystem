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
package de.eintosti.buildsystem.api.world.builder;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for managing builders in a {@link de.eintosti.buildsystem.api.world.BuildWorld}.
 */
public interface Builders {

    /**
     * Checks if the world has a creator.
     *
     * @return {@code true} if the world has a creator, {@code false} otherwise
     */
    boolean hasCreator();

    /**
     * Gets the creator of the world.
     *
     * @return The creator of the world, or {@code null} if there is none
     */
    @Nullable
    Builder getCreator();

    /**
     * Sets the creator of the world.
     *
     * @param creator The new creator
     */
    void setCreator(@Nullable Builder creator);

    /**
     * Checks if the given player is the creator of the world.
     *
     * @param player The player to check
     * @return {@code true} if the player is the creator, {@code false} otherwise
     */
    boolean isCreator(Player player);

    /**
     * Gets an unmodifiable list of all builders.
     *
     * @return List of builders
     */
    @NotNull
    Collection<Builder> getAllBuilders();

    /**
     * Gets a builder by their UUID.
     *
     * @param uuid The UUID to search for
     * @return The builder if found, {@code null} otherwise
     */
    @Nullable
    Builder getBuilder(UUID uuid);

    /**
     * Get a list of all {@link Builder} names
     *
     * @return A list of all builder names
     */
    List<String> getBuilderNames();

    /**
     * Checks if a player is a builder.
     *
     * @param player The player to check
     * @return {@code true} if the player is a builder, {@code false} otherwise
     */
    boolean isBuilder(Player player);

    /**
     * Checks if a UUID belongs to a builder.
     *
     * @param uuid The UUID to check
     * @return {@code true} if the given UUID belongs to a builder, {@code false} otherwise
     */
    boolean isBuilder(UUID uuid);

    /**
     * Adds a builder to the world.
     *
     * @param builder The builder to add
     */
    void addBuilder(@NotNull Builder builder);

    /**
     * Removes a builder from the world.
     *
     * @param builder The builder to remove
     */
    void removeBuilder(@NotNull Builder builder);

    /**
     * Removes a builder by their UUID.
     *
     * @param uuid The UUID of the builder to remove
     */
    void removeBuilder(@NotNull UUID uuid);

    /**
     * Formats the list of builders for the {@code %builder%} placeholder.
     *
     * @param player The player to display the placeholders to
     * @return The list of builders which have been added to the given world as a string
     */
    String asPlaceholder(Player player);
}
