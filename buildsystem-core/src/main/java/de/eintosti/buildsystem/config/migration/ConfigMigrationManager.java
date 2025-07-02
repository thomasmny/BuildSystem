/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * Manages the migration of configurations. It holds a registry of available migration steps and applies them sequentially.
 */
@NullMarked
public class ConfigMigrationManager {

    public static final int LATEST_VERSION = 2;

    private final BuildSystemPlugin plugin;
    private final Map<Integer, Migration> migrations;

    /**
     * Constructs a new {@link ConfigMigrationManager} instance.
     *
     * @param plugin The BuildSystemPlugin instance
     */
    public ConfigMigrationManager(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.migrations = new HashMap<>();

        registerMigration(1, new MigrationV1ToV2());
    }

    /**
     * Registers a {@link Migration} step.
     *
     * @param fromVersion The version from which this migration applies. For example, {@link MigrationV1ToV2} is registered with {@code fromVersion = 1}
     * @param migration   The migration instance
     */
    public void registerMigration(int fromVersion, Migration migration) {
        this.migrations.put(fromVersion, migration);
    }

    /**
     * Migrates the given configuration to the latest version.
     *
     * @throws IllegalStateException If a required migration step is missing.
     */
    public void migrate() {
        Logger logger = plugin.getLogger();

        while (Config.getVersion() < LATEST_VERSION) {
            int currentVersion = Config.getVersion();
            Migration migration = migrations.get(currentVersion);
            if (migration == null) {
                throw new IllegalStateException("Missing migration from version " + currentVersion + " to " + (currentVersion + 1));
            }

            logger.info("Migrating from version " + currentVersion + " to " + (currentVersion + 1) + "...");
            migration.migrate(Config.getConfig());
            Config.setVersion(currentVersion + 1);
        }

        logger.info("Config is at the latest version: " + Config.getVersion());
    }
}
