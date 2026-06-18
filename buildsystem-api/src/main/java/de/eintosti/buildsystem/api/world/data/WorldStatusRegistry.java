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
package de.eintosti.buildsystem.api.world.data;

import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

/**
 * The registry of all {@link BuildWorldStatus statuses} known to the server, both the built-in defaults and any custom
 * statuses created by administrators. Statuses are resolved from their persisted id through this registry.
 *
 * @since TODO
 */
@NullMarked
public interface WorldStatusRegistry {

    /**
     * The id of the built-in {@code not_started} status, which is also the global default fallback.
     */
    String NOT_STARTED_ID = "not_started";

    /**
     * The id of the built-in {@code in_progress} status.
     */
    String IN_PROGRESS_ID = "in_progress";

    /**
     * The id of the built-in {@code almost_finished} status.
     */
    String ALMOST_FINISHED_ID = "almost_finished";

    /**
     * The id of the built-in {@code finished} status.
     */
    String FINISHED_ID = "finished";

    /**
     * The id of the built-in {@code archive} status (building disabled).
     */
    String ARCHIVE_ID = "archive";

    /**
     * The id of the built-in {@code hidden} status (not shown in the navigator).
     */
    String HIDDEN_ID = "hidden";

    /**
     * Gets all registered statuses, ordered by {@link BuildWorldStatus#getOrder()}.
     *
     * @return An unmodifiable view of all statuses
     */
    @Unmodifiable
    Collection<BuildWorldStatus> getStatuses();

    /**
     * Resolves a status by its {@link BuildWorldStatus#getId() id}.
     *
     * @param id The status id
     * @return The matching status, or {@link Optional#empty()} if none is registered with that id
     */
    Optional<BuildWorldStatus> getStatus(String id);

    /**
     * Gets the global default status worlds fall back to (the built-in {@code not_started}). Always present.
     *
     * @return The default status
     */
    BuildWorldStatus getDefaultStatus();
}
