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
package de.eintosti.buildsystem.storage.yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * The YAML file plumbing — a single backing file, its loaded {@link FileConfiguration}, and the I/O lock that
 * serializes access to both — held by each storage rather than inherited. Composing this collapses the parallel
 * plumbing that single inheritance previously forced the cache-backed storages to duplicate (each re-implementing the
 * {@code file}/{@code config}/{@code ioLock}/{@code save} fields and methods because they already extend a cache base).
 *
 * <p>The cache-backed storages mutate and persist off the main thread, so writes go through {@link #atomicSave} and
 * read/load sequences through {@link #locked}; both hold the same lock, so the non-thread-safe configuration is never
 * mutated or written by two tasks at once.
 */
@NullMarked
public final class YamlStore {

    private final File file;
    private final FileConfiguration configuration;
    private final Logger logger;
    private final Object ioLock = new Object();

    public YamlStore(File dataFolder, String fileName, Logger logger) {
        this.file = new File(dataFolder, fileName);
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.logger = logger;
    }

    public FileConfiguration config() {
        return configuration;
    }

    /**
     * Re-reads the backing file from disk, creating an empty one when it does not yet exist.
     *
     * @return {@code true} if the file existed and was read, {@code false} if it was just created
     */
    public boolean reload() {
        if (!file.exists()) {
            configuration.options().copyDefaults(true);
            save();
            return false;
        }

        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, "Failed to load configuration file: " + file.getName(), e);
        }
        return true;
    }

    /**
     * Writes the configuration to disk. Callers needing this synchronized with a preceding mutation use
     * {@link #atomicSave}.
     *
     * <p>Writes to a sibling temp file first and moves it onto {@code file}, so a crash or power loss mid-write
     * never leaves a truncated file in place — {@link #reload} either reads the previous complete file or the new
     * complete one, never a partial one.
     */
    public void save() {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            configuration.save(temp);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration file: %s".formatted(file.getName()), e);
            deleteQuietly(temp);
            return;
        }

        Path tempPath = temp.toPath();
        Path targetPath = file.toPath();
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                logger.log(Level.SEVERE, "Failed to save configuration file: %s".formatted(file.getName()), e2);
                deleteQuietly(temp);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration file: %s".formatted(file.getName()), e);
            deleteQuietly(temp);
        }
    }

    private void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
            // A leftover temp file is harmless: the next successful save overwrites it.
        }
    }

    /**
     * Applies {@code mutation} to the configuration and persists it, both under the I/O lock guarding the file.
     */
    public void atomicSave(Runnable mutation) {
        synchronized (ioLock) {
            mutation.run();
            save();
        }
    }

    /**
     * Runs {@code work} under the I/O lock — for read/load sequences that must not race a concurrent save.
     */
    public <T> T locked(Supplier<T> work) {
        synchronized (ioLock) {
            return work.get();
        }
    }

    /**
     * Copies the backing file to a sibling {@code <name><suffix>} once, before a destructive rewrite such as a format
     * migration. Does nothing if the backup already exists, so the original (pre-migration) snapshot is never
     * overwritten by a later run.
     *
     * @param suffix The suffix appended to the file name for the backup (e.g. {@code .v3.bak})
     */
    public void backupOnce(String suffix) {
        File backup = new File(file.getParentFile(), file.getName() + suffix);
        if (backup.exists()) {
            return;
        }
        try {
            Files.copy(file.toPath(), backup.toPath());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to back up " + file.getName() + " to " + backup.getName(), e);
        }
    }
}
