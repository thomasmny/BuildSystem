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

import de.eintosti.buildsystem.config.PluginConfig.World.Backup.Local;
import de.eintosti.buildsystem.config.PluginConfig.World.Backup.S3;
import de.eintosti.buildsystem.config.PluginConfig.World.Backup.Sftp;
import de.eintosti.buildsystem.config.PluginConfig.World.Backup.StorageSettings;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Builds the backup {@link StorageSettings} from config, validating that a chosen remote backend actually has its
 * required credentials before selecting it. A misconfigured remote logs exactly which key is missing and falls back to
 * {@link Local local} storage, rather than constructing a remote with {@code null} fields that fails only later at
 * connection time.
 */
@NullMarked
final class StorageSettingsFactory {

    private static final String BASE = "world.backup.storage.";

    private StorageSettingsFactory() {}

    static StorageSettings fromConfig(FileConfiguration config, Logger logger) {
        String type = config.getString(BASE + "type", "local").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "s3" -> s3(config, logger);
            case "sftp" -> sftp(config, logger);
            case "local" -> new Local();
            default -> {
                logger.warning("Unknown backup storage type '" + type + "', defaulting to local storage.");
                yield new Local();
            }
        };
    }

    private static StorageSettings s3(FileConfiguration config, Logger logger) {
        String prefix = BASE + "s3.";
        Map<String, String> required = new LinkedHashMap<>();
        required.put(prefix + "url", config.getString(prefix + "url"));
        required.put(prefix + "access-key", config.getString(prefix + "access-key"));
        required.put(prefix + "secret-key", config.getString(prefix + "secret-key"));
        required.put(prefix + "bucket", config.getString(prefix + "bucket"));

        if (fallbackOnMissing(required, "s3", logger)) {
            return new Local();
        }
        return new S3(
                config.getString(prefix + "url"),
                config.getString(prefix + "access-key"),
                config.getString(prefix + "secret-key"),
                config.getString(prefix + "region"),
                config.getString(prefix + "bucket"),
                config.getString(prefix + "path"));
    }

    private static StorageSettings sftp(FileConfiguration config, Logger logger) {
        String prefix = BASE + "sftp.";
        Map<String, String> required = new LinkedHashMap<>();
        required.put(prefix + "host", config.getString(prefix + "host"));
        required.put(prefix + "username", config.getString(prefix + "username"));
        required.put(prefix + "password", config.getString(prefix + "password"));

        if (fallbackOnMissing(required, "sftp", logger)) {
            return new Local();
        }

        return new Sftp(
                config.getString(prefix + "host"),
                config.getInt(prefix + "port", 22),
                config.getString(prefix + "username"),
                config.getString(prefix + "password"),
                config.getString(prefix + "path"));
    }

    /**
     * {@return whether a required value is missing} Logs the first blank/absent key together with the backend name when
     * so, signalling the caller to fall back to local storage.
     */
    private static boolean fallbackOnMissing(Map<String, String> required, String backend, Logger logger) {
        for (Map.Entry<String, String> entry : required.entrySet()) {
            if (isBlank(entry.getValue())) {
                logger.warning("Backup storage '" + backend + "' is missing required setting '" + entry.getKey()
                        + "'; falling back to local storage.");
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
