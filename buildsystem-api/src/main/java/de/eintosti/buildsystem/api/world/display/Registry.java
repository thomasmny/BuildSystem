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
package de.eintosti.buildsystem.api.world.display;

import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The admin-managed set of {@link RegistryEntry entries} of one kind — world statuses or navigator categories.
 * Entries are keyed by their {@link RegistryEntry#getId() id} and kept in the order they are shown.
 *
 * <p>A registry is never empty: deleting the last remaining entry is refused, so {@link #getDefault()} always has
 * something to return.
 *
 * @param <T> The entry type
 * @since 4.0.0
 */
@NullMarked
public interface Registry<T extends RegistryEntry> {

    /**
     * Gets every entry, in display order.
     *
     * @return The entries
     */
    @Unmodifiable
    Collection<T> getAll();

    /**
     * Gets the entry with the given id.
     *
     * @param id The id to look up, may be {@code null}
     * @return The entry, or empty if no entry has that id
     */
    Optional<T> get(@Nullable String id);

    /**
     * Gets the entry used when none is specified — for a status, what a newly created world starts as; for a
     * category, where an uncategorised world is listed.
     *
     * @return The default entry
     */
    T getDefault();
}
