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
package de.eintosti.buildsystem.config.migration;

import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Migrates config from v4 to v5: the S3 and SFTP credentials move out of {@code world.backup.storage} and up to a root
 * {@code storage} section.
 *
 * <p>They were only ever nested under backups because backups needed them first. World downloads read the same bucket,
 * and a second feature reading a key path named after the first is a lie that only gets more expensive. The backend a
 * feature uses is now named by {@code world.backup.storage} directly, and the prefix it writes under moves with the
 * feature rather than with the credentials.
 */
@NullMarked
public class MigrationV4ToV5 implements Migration {

    private static final String OLD_BASE = "world.backup.storage";

    @Override
    public void migrate(FileConfiguration config) {
        String type = config.getString(OLD_BASE + ".type", "local").toLowerCase(Locale.ROOT);

        move(config, OLD_BASE + ".s3.url", "storage.s3.url");
        move(config, OLD_BASE + ".s3.access-key", "storage.s3.access-key");
        move(config, OLD_BASE + ".s3.secret-key", "storage.s3.secret-key");
        move(config, OLD_BASE + ".s3.region", "storage.s3.region");
        move(config, OLD_BASE + ".s3.bucket", "storage.s3.bucket");

        move(config, OLD_BASE + ".sftp.host", "storage.sftp.host");
        move(config, OLD_BASE + ".sftp.port", "storage.sftp.port");
        move(config, OLD_BASE + ".sftp.username", "storage.sftp.username");
        move(config, OLD_BASE + ".sftp.password", "storage.sftp.password");

        // Both backends carried their own path; only the one actually in use describes where backups really are.
        String path = config.getString(OLD_BASE + "." + type + ".path");
        if (path != null && !path.isBlank()) {
            config.set("world.backup.path", path);
        }

        // Replacing the section with a scalar of the same name: the old subtree has to go first, or the type would be
        // written as a child of the very section it replaces.
        config.set(OLD_BASE, null);
        config.set(OLD_BASE, type);
        config.setComments(
                OLD_BASE, List.of("Where backups are written: local, s3 or sftp.", "Credentials live under storage."));
    }

    /**
     * Copies a value to its new key and drops the old one, leaving an absent value absent rather than writing a null.
     */
    private static void move(FileConfiguration config, String from, String to) {
        @Nullable Object value = config.get(from);
        if (value != null) {
            config.set(to, value);
        }
        config.set(from, null);
    }
}
