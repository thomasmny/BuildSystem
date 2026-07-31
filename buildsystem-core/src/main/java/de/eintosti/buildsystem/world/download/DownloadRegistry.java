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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The live download links: which archive each token stands for, when it dies, and who is allowed to fetch it.
 *
 * <p>A link is a bearer token, so the first client to use one claims it — later requests from a different address are
 * refused. That keeps a forwarded or shoulder-surfed link from being useful to anyone but the player who asked for it,
 * while still allowing that player to retry or resume.
 */
@NullMarked
final class DownloadRegistry {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Download> downloads = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    DownloadRegistry() {
        this(System::currentTimeMillis);
    }

    DownloadRegistry(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Registers an archive and returns the token it is reachable under.
     *
     * @param file The archive on disk
     * @param fileName The name the client should save it as
     * @param expiresAt When the link dies, in epoch milliseconds
     * @return The generated token
     */
    String register(Path file, String fileName, long expiresAt) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        downloads.put(token, new Download(file, fileName, expiresAt, null));
        return token;
    }

    /**
     * Resolves a token for the client asking for it, pinning the link to that client on first use.
     *
     * @param token The token from the request
     * @param clientAddress The requesting client's address
     * @return The download, or {@code null} if the token is unknown, expired, or claimed by another client
     */
    @Nullable Download claim(String token, String clientAddress) {
        Download claimed = downloads.computeIfPresent(token, (ignored, download) -> {
            if (download.isExpired(clock.getAsLong())) {
                return download;
            }
            return download.claimedBy() == null ? download.claimFor(clientAddress) : download;
        });

        if (claimed == null || claimed.isExpired(clock.getAsLong()) || !clientAddress.equals(claimed.claimedBy())) {
            return null;
        }
        return claimed;
    }

    /**
     * Drops every expired link, handing each archive to {@code deleter}.
     */
    void purgeExpired(Consumer<Path> deleter) {
        downloads.values().removeIf(download -> {
            if (!download.isExpired(clock.getAsLong())) {
                return false;
            }
            deleter.accept(download.file());
            return true;
        });
    }

    /**
     * {@return the total size of the registered archives} Used to hold all live downloads under the configured
     * storage budget.
     */
    long totalBytes() {
        return downloads.values().stream().mapToLong(Download::size).sum();
    }

    void clear() {
        downloads.clear();
    }

    /**
     * @param file The archive on disk
     * @param fileName The name the client saves it as
     * @param expiresAt When the link dies, in epoch milliseconds
     * @param claimedBy The address that first used the link, or {@code null} while it is unclaimed
     */
    record Download(
            Path file,
            String fileName,
            long expiresAt,
            @Nullable String claimedBy) {

        boolean isExpired(long now) {
            return now > expiresAt;
        }

        Download claimFor(String clientAddress) {
            return new Download(file, fileName, expiresAt, clientAddress);
        }

        long size() {
            try {
                return Files.size(file);
            } catch (IOException e) {
                return 0L;
            }
        }
    }
}
