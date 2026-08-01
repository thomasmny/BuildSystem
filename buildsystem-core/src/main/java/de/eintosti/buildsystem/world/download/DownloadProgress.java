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
package de.eintosti.buildsystem.world.download;

import org.jspecify.annotations.NullMarked;

/**
 * Notified as a world is made downloadable. Carries the phase so the player sees which step is running, rather than a
 * bar that fills and then sits there while a later step works.
 */
@NullMarked
@FunctionalInterface
public interface DownloadProgress {

    /**
     * @param phase What is happening now
     * @param done How far the phase has got, in bytes
     * @param total How many bytes the phase covers, or zero while that is not yet known
     */
    void update(Phase phase, long done, long total);

    /**
     * The steps a download goes through, in order.
     */
    enum Phase {

        /**
         * The world is being packed into an archive.
         */
        PACKING,

        /**
         * The archive is being put where the player can fetch it. Instant when it is served locally, as long as the
         * uplink needs when it is uploaded.
         */
        PUBLISHING
    }
}
