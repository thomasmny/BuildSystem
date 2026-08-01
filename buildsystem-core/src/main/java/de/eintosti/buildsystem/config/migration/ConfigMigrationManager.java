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

import de.eintosti.buildsystem.BuildSystemPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * Manages the migration of configurations. It holds a registry of available migration steps and applies them
 * sequentially.
 */
@NullMarked
public class ConfigMigrationManager {

    public static final int LATEST_VERSION = 5;

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
        registerMigration(2, new MigrationV2ToV3());
        registerMigration(3, new MigrationV3ToV4());
        registerMigration(4, new MigrationV4ToV5());
    }

    /**
     * Registers a {@link Migration} step.
     *
     * @param fromVersion The version from which this migration applies. For example, {@link MigrationV1ToV2} is
     *     registered with {@code fromVersion = 1}
     * @param migration The migration instance
     */
    private void registerMigration(int fromVersion, Migration migration) {
        this.migrations.put(fromVersion, migration);
    }

    /**
     * Migrates the given configuration to the latest version.
     *
     * @throws IllegalStateException If a required migration step is missing.
     */
    public void migrate() {
        Logger logger = plugin.getLogger();
        int fromVersion = plugin.getConfig().getInt("version", 1);
        if (fromVersion >= LATEST_VERSION) {
            logger.info("Config is at the latest version: %d".formatted(fromVersion));
            return;
        }

        if (!backupConfig(fromVersion, logger)) {
            return;
        }

        while (plugin.getConfig().getInt("version", 1) < LATEST_VERSION) {
            int currentVersion = plugin.getConfig().getInt("version", 1);
            Migration migration = migrations.get(currentVersion);
            if (migration == null) {
                throw new IllegalStateException(
                        "Missing migration from version %d to %d".formatted(currentVersion, currentVersion + 1));
            }

            logger.info("Migrating from version %d to %d...".formatted(currentVersion, currentVersion + 1));
            migration.migrate(plugin.getConfig());
            plugin.getConfig().set("version", currentVersion + 1);
            plugin.getConfig().setComments("version", List.of("Internal, do not change manually!"));
            plugin.saveConfig();
        }

        logger.info("Config is at the latest version: %d"
                .formatted(plugin.getConfig().getInt("version", 1)));
    }

    /**
     * Copies {@code config.yml} to a sibling {@code config.yml.v<fromVersion>.bak} before the first migration
     * mutates it in place.
     *
     * @return {@code true} if the backup succeeded (or the config file does not exist yet), {@code false} if
     *     migration must be aborted because the pre-migration state could not be preserved
     */
    private boolean backupConfig(int fromVersion, Logger logger) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return true;
        }

        File backup = new File(plugin.getDataFolder(), "config.yml.v%d.bak".formatted(fromVersion));
        try {
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(
                    Level.SEVERE,
                    "Failed to back up config.yml to %s; aborting migration".formatted(backup.getName()),
                    e);
            return false;
        }

        logger.info("Backed up config.yml to %s".formatted(backup.getName()));
        return true;
    }
}
