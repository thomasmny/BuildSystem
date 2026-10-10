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

import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Migrates config from v6 to v7: adds {@code storage.type}, which picks where worlds, folders and players are kept,
 * with the prefix and the connection block of every database. A server that already has worlds or players in YAML
 * keeps using YAML; the shipped default is SQLite. The keys are written here rather than left to the copied defaults
 * because those carry no comments, and the comments say what is unsafe to change.
 */
@NullMarked
public class MigrationV6ToV7 implements Migration {

    private final File dataFolder;

    public MigrationV6ToV7(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void migrate(FileConfiguration config) {
        if (config.contains("storage.type", true)) {
            return;
        }
        boolean hasYamlData =
                new File(dataFolder, "worlds.yml").exists() || new File(dataFolder, "players.yml").exists();
        config.set("storage.type", hasYamlData ? "yaml" : "sqlite");
        config.setComments(
                "storage.type",
                List.of(
                        "Where worlds, folders and player settings are kept: yaml, sqlite, mysql,",
                        "mariadb, postgresql or mongodb. sqlite keeps them in storage.db in the",
                        "plugin folder, the others on the server set in their block below. World",
                        "folders always stay on disk, and categories, statuses, setup and spawn always",
                        "stay in their YAML files.",
                        "To switch, run /buildsystem storage migrate <type>, which copies the current",
                        "data over and switches without a restart, or change this and restart.",
                        "On startup, each new table or collection is filled from worlds.yml,",
                        "folders.yml and players.yml. Those files are kept but no longer updated, so",
                        "changing this back to yaml and restarting loads them as they were at the",
                        "switch, while migrating to yaml writes the current data into them.",
                        "If the database cannot be reached, BuildSystem disables itself rather than",
                        "fall back to yaml."));
        config.set("storage.table-prefix", "buildsystem_");
        config.setComments(
                "storage.table-prefix",
                List.of(
                        "Put in front of every table and collection name: lowercase letters, digits",
                        "and underscores, at most 32 and not starting with a digit. Every server needs",
                        "its own prefix or its own database. Each server loads the data once on startup",
                        "and keeps it in memory, so two servers sharing a prefix overwrite each other's",
                        "worlds, folders and players."));
        server(config, "mysql", 3306);
        config.setComments(
                "storage.mysql",
                List.of(
                        "Each password can also be supplied as an environment variable named after",
                        "its block, e.g. BUILDSYSTEM_MYSQL_PASSWORD, which takes precedence and keeps",
                        "the secret out of this file.",
                        "tls sets whether the connection is encrypted: prefer (when the server offers",
                        "it), off, require (always, without checking the certificate) or verify",
                        "(always, checking the certificate and host name). verify trusts what the Java",
                        "trust store trusts, or ~/.postgresql/root.crt for PostgreSQL. MongoDB cannot",
                        "fall back to plain text, so there prefer does what off does, and require still",
                        "checks the certificate, only not the host name."));
        server(config, "mariadb", 3306);
        server(config, "postgresql", 5432);
        server(config, "mongodb", 27017);
        config.setComments(
                "storage.mongodb",
                List.of(
                        "Leave username empty for a server without authentication. Users are",
                        "authenticated against the admin database."));
    }

    private static void server(FileConfiguration config, String type, int port) {
        String block = "storage." + type + ".";
        config.set(block + "host", "localhost");
        config.set(block + "port", port);
        config.set(block + "database", "buildsystem");
        config.set(block + "username", "");
        config.set(block + "password", "");
        config.set(block + "tls", "prefer");
    }
}
