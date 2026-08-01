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
package de.eintosti.buildsystem.world.backup.storage.s3;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the multipart split, which decides whether a large world can be uploaded at all: too small a part and the
 * upload needs more than the ten thousand parts S3 allows, too small a last part and S3 rejects it.
 */
class S3ClientPartSizeTest {

    private static final long MIB = 1024L * 1024L;
    private static final long MIN_PART_SIZE = 16 * MIB;
    private static final long MAX_PARTS = 10_000L;

    @Test
    @DisplayName("A world that fits in the default part count keeps the base part size")
    void smallWorldsUseTheBasePartSize() {
        assertEquals(MIN_PART_SIZE, S3Client.partSize(0L));
        assertEquals(MIN_PART_SIZE, S3Client.partSize(MIB));
        assertEquals(MIN_PART_SIZE, S3Client.partSize(2048 * MIB));
        assertEquals(MIN_PART_SIZE, S3Client.partSize(MIN_PART_SIZE * MAX_PARTS));
    }

    @Test
    @DisplayName("Past 160 GB the part size grows instead of the part count")
    void hugeWorldsGrowThePartSize() {
        long justOver = MIN_PART_SIZE * MAX_PARTS + 1;
        assertTrue(S3Client.partSize(justOver) > MIN_PART_SIZE, "part size must grow once 10000 parts is not enough");
        assertTrue(partCount(justOver) <= MAX_PARTS, "must never need more than 10000 parts");
    }

    @Test
    @DisplayName("No world size needs more parts than S3 accepts")
    void partCountNeverExceedsTheLimit() {
        long[] sizes = {
            1L, MIB, 1024 * MIB, 100L * 1024 * MIB, 1024L * 1024 * MIB, Long.MAX_VALUE / 2,
        };
        for (long size : sizes) {
            assertTrue(partCount(size) <= MAX_PARTS, "too many parts for a size of " + size);
        }
    }

    @Test
    @DisplayName("Every part but the last clears the five-megabyte minimum S3 enforces")
    void partsClearTheServiceMinimum() {
        assertTrue(S3Client.partSize(1L) >= 5 * MIB);
        assertTrue(S3Client.partSize(Long.MAX_VALUE / 2) >= 5 * MIB);
    }

    private static long partCount(long size) {
        long partSize = S3Client.partSize(size);
        return (size + partSize - 1) / partSize;
    }
}
