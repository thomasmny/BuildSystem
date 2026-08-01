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

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MigrationV4ToV5Test {

    @Test
    void migrate_s3Backend_movesCredentialsToRootAndKeepsItsPath() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.backup.storage.type", "s3");
        config.set("world.backup.storage.s3.url", "https://s3.example.com");
        config.set("world.backup.storage.s3.access-key", "KEY");
        config.set("world.backup.storage.s3.secret-key", "SECRET");
        config.set("world.backup.storage.s3.region", "eu-central-1");
        config.set("world.backup.storage.s3.bucket", "my-bucket");
        config.set("world.backup.storage.s3.path", "backups/worlds/");

        new MigrationV4ToV5().migrate(config);

        assertEquals("https://s3.example.com", config.getString("storage.s3.url"));
        assertEquals("KEY", config.getString("storage.s3.access-key"));
        assertEquals("SECRET", config.getString("storage.s3.secret-key"));
        assertEquals("eu-central-1", config.getString("storage.s3.region"));
        assertEquals("my-bucket", config.getString("storage.s3.bucket"));
        assertEquals("backups/worlds/", config.getString("world.backup.path"));

        // The section is gone and its name now holds the backend, not a subtree.
        assertEquals("s3", config.getString("world.backup.storage"));
        assertNull(config.get("world.backup.storage.s3"));
        assertNull(config.get("world.backup.storage.type"));
    }

    @Test
    void migrate_sftpBackend_movesCredentialsAndTakesTheSftpPath() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.backup.storage.type", "sftp");
        config.set("world.backup.storage.sftp.host", "sftp.example.com");
        config.set("world.backup.storage.sftp.port", 2222);
        config.set("world.backup.storage.sftp.username", "user");
        config.set("world.backup.storage.sftp.password", "pass");
        config.set("world.backup.storage.sftp.path", "/srv/backups/");
        // A leftover S3 path from an earlier experiment must not win over the backend actually in use.
        config.set("world.backup.storage.s3.path", "wrong/");

        new MigrationV4ToV5().migrate(config);

        assertEquals("sftp.example.com", config.getString("storage.sftp.host"));
        assertEquals(2222, config.getInt("storage.sftp.port"));
        assertEquals("user", config.getString("storage.sftp.username"));
        assertEquals("pass", config.getString("storage.sftp.password"));
        assertEquals("/srv/backups/", config.getString("world.backup.path"));
        assertEquals("sftp", config.getString("world.backup.storage"));
    }

    @Test
    void migrate_localBackend_carriesNoPathOver() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.backup.storage.type", "local");
        config.set("world.backup.storage.s3.path", "unused/");

        new MigrationV4ToV5().migrate(config);

        assertEquals("local", config.getString("world.backup.storage"));
        assertNull(config.get("world.backup.path"));
    }

    @Test
    void migrate_missingSection_defaultsToLocalAndWritesNoCredentials() {
        YamlConfiguration config = new YamlConfiguration();

        new MigrationV4ToV5().migrate(config);

        assertEquals("local", config.getString("world.backup.storage"));
        assertNull(config.get("storage"));
    }
}
