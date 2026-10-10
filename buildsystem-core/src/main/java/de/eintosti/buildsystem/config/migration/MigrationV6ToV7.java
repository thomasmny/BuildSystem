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
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Migrates config from v6 to v7: adds {@code storage.database}, which picks where worlds, folders and players are kept.
 * The values are the shipped ones, so a migrated server keeps using YAML. The section is written here rather than left
 * to the copied defaults because those carry no comments, and the comments say what is unsafe to change.
 */
@NullMarked
public class MigrationV6ToV7 implements Migration {

    private static final String BASE = "storage.database";

    @Override
    public void migrate(FileConfiguration config) {
        if (config.contains(BASE, true)) {
            return;
        }
        set(config, "type", "yaml");
        set(config, "host", "localhost");
        set(config, "port", 0);
        set(config, "database", "buildsystem");
        set(config, "username", "");
        set(config, "password", "");
        set(config, "table-prefix", "buildsystem_");

        config.setComments(
                BASE,
                List.of(
                        "Where worlds, folders and player settings are kept: yaml, sqlite, mysql,",
                        "mariadb, postgresql or mongodb. Read on startup only, so a change needs a",
                        "restart. World folders always stay on disk, and categories, statuses, setup",
                        "and spawn always stay in their YAML files.",
                        "The first start with a database fills each empty table or collection from",
                        "worlds.yml, folders.yml and players.yml. Those files are kept but no longer",
                        "updated, so switching back to yaml loads them as they were at the switch.",
                        "If the database cannot be reached, BuildSystem disables itself rather than",
                        "fall back to yaml."));
        config.setComments(
                BASE + ".host", List.of("Not used by yaml or sqlite. Port 0 is the default port of the type."));
        config.setComments(
                BASE + ".database",
                List.of("The database to use. For sqlite, the name of the file in the plugin folder."));
        config.setComments(
                BASE + ".username",
                List.of(
                        "Leave empty for a MongoDB server without authentication. MongoDB users",
                        "are authenticated against the admin database."));
        config.setComments(
                BASE + ".password",
                List.of(
                        "Can also be supplied as the BUILDSYSTEM_DATABASE_PASSWORD environment",
                        "variable, which takes precedence and keeps the secret out of this file."));
        config.setComments(
                BASE + ".table-prefix",
                List.of(
                        "Every server needs its own prefix or its own database. Each server loads",
                        "the data once on startup and keeps it in memory, so two servers sharing",
                        "a prefix overwrite each other's worlds, folders and players."));
    }

    private static void set(FileConfiguration config, String key, Object value) {
        config.set(BASE + "." + key, value);
    }
}
