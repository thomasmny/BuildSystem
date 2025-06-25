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

import java.util.UUID;

/**
 * Concrete implementation of the {@link Builder} interface.
 *
 * @since 3.0.0
 */
final class BuilderImpl implements Builder {

    static final String SEPARATOR = ",";

    private final UUID uuid;
    private String name;

    /**
     * Constructs a new {@link BuilderImpl} with the given unique ID and name.
     *
     * @param uuid The unique ID of the builder
     * @param name The name of the builder
     */
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
        return uuid.toString() + SEPARATOR + name;
    }
}