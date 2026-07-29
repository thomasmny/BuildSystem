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

    /**
     * Creates a new entry with the given display name, assigns it an id, and persists it. The entry starts hidden and
     * unplaced; give it a slot with {@link RegistryEntry#setSlot(int)} and show it with
     * {@link RegistryEntry#setShown(boolean)}.
     *
     * @param displayName The display name to create it under
     * @return The created entry
     */
    T create(String displayName);

    /**
     * Deletes the entry with the given id, cascading to whatever referenced it so nothing is left pointing at an id
     * this registry no longer resolves.
     *
     * <p>Whether the last remaining entry may be deleted is up to the implementation, so callers must not assume the
     * registry stays non-empty: a status registry keeps its final status (every world needs one), while a category
     * registry may be emptied completely, leaving the navigator blank until the built-ins are restored.
     *
     * @param id The id to delete
     * @return {@code true} if it was deleted, {@code false} if the id was unknown or the implementation refused
     */
    boolean delete(String id);

    /**
     * Writes an entry's current state to storage. Mutating an entry does not save it on its own, so a drag that
     * touches several fields costs one write rather than one per field.
     *
     * @param entry The entry to persist
     */
    void persist(T entry);

    /**
     * Restores the built-in entries to their default slots and hides the rest, keeping custom entries.
     */
    void resetLayout();

    /**
     * Restores every entry to the built-in defaults, discarding custom entries. Discarding one cascades exactly as
     * {@link #delete(String)} does, so nothing keeps a reference to an entry this registry no longer resolves.
     */
    void resetToDefaults();
}
