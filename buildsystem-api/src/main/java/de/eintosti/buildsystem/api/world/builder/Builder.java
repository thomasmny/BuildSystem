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

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Builder} represents a player allowed to build in a {@link BuildWorld}.
 *
 * @since 3.0.0
 */
@NullMarked
public sealed interface Builder permits BuilderImpl {

    /**
     * Creates a new {@link Builder} instance with the given uuid and name.
     *
     * @param uuid The uuid
     * @param name The name
     * @return The builder
     */
    @Contract("_, _ -> new")
    static Builder of(UUID uuid, String name) {
        return new BuilderImpl(uuid, name);
    }

    /**
     * Creates a new {@link Builder} instance using the given player.
     *
     * @param player The player
     * @return The builder
     */
    @Contract("_ -> new")
    static Builder of(Player player) {
        return of(player.getUniqueId(), player.getName());
    }

    /**
     * Creates a new {@link Builder} instance using a serialized string.
     * <p>
     * The format of the string must be {@code <uuid>,<name>}.
     *
     * @param serialized The serialized builder
     * @return The builder if all the input is valid, otherwise {@code null}
     */
    @Nullable
    static Builder deserialize(@Nullable String serialized) {
        if (serialized == null || serialized.equals("-")) {
            return null;
        }

        String[] parts = serialized.split(BuilderImpl.SEPARATOR);
        if (parts.length != 2) {
            return null;
        }

        return of(UUID.fromString(parts[0]), parts[1]);
    }

    /**
     * Returns a unique and persistent id for the builder.
     * <p>
     * Should be equal to the corresponding {@link Player}'s unique id.
     *
     * @return The uuid
     * @see Player#getUniqueId()
     */
    UUID getUniqueId();

    /**
     * Gets the name of the builder.
     *
     * @return The builder name
     */
    String getName();

    /**
     * Sets the name of the builder.
     *
     * @param name The name to change to
     */
    void setName(String name);
}