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
package de.eintosti.buildsystem.config;

import de.eintosti.buildsystem.config.PluginConfig.Storage;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reads the root {@code storage} section and resolves which backend a feature may use.
 *
 * <p>A backend is only selected once its credentials are actually present: a misconfigured one logs exactly which key
 * is missing and falls back to {@link Storage.Type#LOCAL local} storage, rather than being handed out and failing later
 * at connection time.
 */
@NullMarked
final class StorageSettingsFactory {

    private static final String BASE = "storage.";

    private StorageSettingsFactory() {}

    /**
     * {@return the credentials for every backend, whether or not a feature selected it}
     */
    static Storage fromConfig(FileConfiguration config) {
        String s3 = BASE + "s3.";
        String sftp = BASE + "sftp.";
        return new Storage(
                new Storage.S3(
                        config.getString(s3 + "url"),
                        config.getString(s3 + "access-key"),
                        config.getString(s3 + "secret-key"),
                        config.getString(s3 + "region"),
                        config.getString(s3 + "bucket")),
                new Storage.Sftp(
                        config.getString(sftp + "host"),
                        config.getInt(sftp + "port", 22),
                        config.getString(sftp + "username"),
                        config.getString(sftp + "password")));
    }

    /**
     * {@return the backend named at {@code path}, or {@link Storage.Type#LOCAL} when it is unknown, unsupported by the
     * feature, or missing its credentials}
     *
     * @param config The raw configuration
     * @param path The key naming the backend, e.g. {@code world.backup.storage}
     * @param storage The parsed credentials, checked for whichever backend was named
     * @param supported The backends this feature can actually use
     * @param logger The plugin logger
     */
    static Storage.Type typeAt(
            FileConfiguration config, String path, Storage storage, Set<Storage.Type> supported, Logger logger) {
        String name =
                Objects.requireNonNullElse(config.getString(path), "local").toLowerCase(Locale.ROOT);
        Storage.Type type =
                switch (name) {
                    case "local" -> Storage.Type.LOCAL;
                    case "s3" -> Storage.Type.S3;
                    case "sftp" -> Storage.Type.SFTP;
                    default -> {
                        logger.warning(
                                "Unknown storage type '" + name + "' at " + path + "; falling back to local storage.");
                        yield Storage.Type.LOCAL;
                    }
                };

        if (type != Storage.Type.LOCAL && !supported.contains(type)) {
            logger.warning(path + " does not support '" + name + "' storage; falling back to local storage.");
            return Storage.Type.LOCAL;
        }
        return hasCredentials(type, storage, logger) ? type : Storage.Type.LOCAL;
    }

    /**
     * {@return whether the backend has everything it needs to connect} Logs the first missing key when it does not.
     */
    private static boolean hasCredentials(Storage.Type type, Storage storage, Logger logger) {
        Map<String, @Nullable String> required = new LinkedHashMap<>();
        switch (type) {
            case LOCAL -> {
                return true;
            }
            case S3 -> {
                // url is optional: absent means AWS rather than an S3-compatible service. The credentials may be
                // supplied through AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY instead of the config.
                required.put(BASE + "s3.region", storage.s3().region());
                required.put(BASE + "s3.bucket", storage.s3().bucket());
                required.put(
                        BASE + "s3.access-key (or AWS_ACCESS_KEY_ID)",
                        storage.s3().resolvedAccessKey());
                required.put(
                        BASE + "s3.secret-key (or AWS_SECRET_ACCESS_KEY)",
                        storage.s3().resolvedSecretKey());
            }
            case SFTP -> {
                // The password may be supplied through BUILDSYSTEM_SFTP_PASSWORD instead of the config.
                required.put(BASE + "sftp.host", storage.sftp().host());
                required.put(BASE + "sftp.username", storage.sftp().username());
                required.put(
                        BASE + "sftp.password (or BUILDSYSTEM_SFTP_PASSWORD)",
                        storage.sftp().resolvedPassword());
            }
        }

        for (Map.Entry<String, @Nullable String> entry : required.entrySet()) {
            if (isBlank(entry.getValue())) {
                logger.warning("Storage '" + type.name().toLowerCase(Locale.ROOT) + "' is missing required setting '"
                        + entry.getKey() + "'; falling back to local storage.");
                return false;
            }
        }
        return true;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
