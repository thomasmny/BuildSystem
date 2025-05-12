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
package de.eintosti.buildsystem.world.builder;

import java.util.UUID;
import org.bukkit.entity.Player;

public class Builder {

    private final UUID uuid;
    private final String name;

    private Builder(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    /**
     * Creates a new {@link Builder} instance with the given uuid and name.
     *
     * @param uuid The uuid
     * @param name The name
     * @return The builder
     */
    public static Builder of(UUID uuid, String name) {
        return new Builder(uuid, name);
    }

    /**
     * Creates a new {@link Builder} instance from a player.
     *
     * @param player The player
     * @return The builder
     */
    public static Builder of(Player player) {
        return new Builder(player.getUniqueId(), player.getName());
    }

    /**
     * Creates a new {@link Builder} instance using a serialized string.
     * <p>
     * The format of the string must be {@code <uuid>:<name>}.
     *
     * @param serialized The serialized builder
     * @return The builder if all the input is valid, otherwise {@code null}
     */
    public static Builder deserialize(String serialized) {
        if (serialized == null || serialized.equals("-")) {
            return null;
        }

        String[] parts = serialized.split(":");
        return new Builder(UUID.fromString(parts[0]), parts[1]);
    }

    public String serialize() {
        return uuid.toString() + ":" + name;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Builder builder = (Builder) o;
        return uuid.equals(builder.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}