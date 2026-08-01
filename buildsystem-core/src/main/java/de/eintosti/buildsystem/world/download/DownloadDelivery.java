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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import org.jspecify.annotations.NullMarked;

/**
 * Turns a packed world into a link and owns whatever that link needs until it expires. {@link WorldDownloadService}
 * does the exporting; where the archive then goes is a delivery's business.
 */
@NullMarked
interface DownloadDelivery extends AutoCloseable {

    /**
     * Makes an archive downloadable.
     *
     * <p>Called off the main thread, and takes ownership of {@code archive}: the delivery either keeps the file until
     * the link expires or deletes it once the bytes are somewhere else.
     *
     * @param archive The packed world
     * @param fileName The name the player should save it as
     * @param lifetime How long the link stays valid, counted from the moment it is handed out rather than from when
     *     the export was asked for, so packing and uploading do not eat into it
     * @param progress Notified with the running total of bytes published, where publishing takes measurable time
     * @return The URL to hand the player
     * @throws IOException If the archive cannot be published
     */
    String publish(Path archive, String fileName, Duration lifetime, ProgressListener progress) throws IOException;

    /**
     * Claims room for one archive against whatever budget this delivery keeps.
     *
     * <p>Reserving rather than merely reading the free space is what keeps two exports started at the same moment from
     * both being told the whole remainder is theirs. Every granted reservation must be handed back to
     * {@link #release(long)}.
     *
     * @return The largest archive the caller may now produce, {@link Long#MAX_VALUE} when the delivery bounds nothing,
     *     or zero or less when it is momentarily full
     */
    long reserve();

    /**
     * Hands back a reservation, whether the archive was published or the export failed.
     *
     * @param reserved What {@link #reserve()} granted
     */
    void release(long reserved);

    /**
     * {@return whether publishing takes long enough to be worth showing the player} False when it only has to
     * register a local file.
     */
    boolean reportsPublishProgress();

    /**
     * Drops everything whose link has expired.
     */
    void purgeExpired();

    @Override
    void close();

    /**
     * Notified as bytes are published.
     */
    @FunctionalInterface
    interface ProgressListener {

        /**
         * @param published How many bytes have been published so far
         * @param total How many there are in total
         */
        void update(long published, long total);
    }
}
