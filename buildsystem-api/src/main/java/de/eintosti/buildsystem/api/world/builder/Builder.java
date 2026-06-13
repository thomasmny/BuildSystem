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
package de.eintosti.buildsystem.api.world.builder;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Builder} represents a player trusted to modify a {@link BuildWorld}.
 *
 * <p>Not to be confused with {@link WorldBuilder}, the fluent API for <em>creating</em> worlds — a {@code Builder} is a
 * person, whereas a {@code WorldBuilder} configures and builds a new world.
 *
 * @since 3.0.0
 */
@NullMarked
public sealed interface Builder permits Builder.BuilderImpl {

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
     *
     * <p>The format of the string must be {@code <uuid>,<name>}.
     *
     * @param serialized The serialized builder
     * @return The builder if all the input is valid, otherwise {@code null}
     */
    static @Nullable Builder deserialize(@Nullable String serialized) {
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
     *
     * <p>Should be equal to the corresponding {@link Player}'s unique id.
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

    /**
     * Default {@link Builder} implementation.
     *
     * <p>Holds the builder's uuid and a mutable name. Nested in the sealed interface so the implementation type stays
     * off the public package surface while still satisfying the {@code permits} clause.
     */
    final class BuilderImpl implements Builder {

        static final String SEPARATOR = ",";

        private final UUID uuid;
        private String name;

        BuilderImpl(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.uuid + SEPARATOR + this.name;
        }
    }
}
