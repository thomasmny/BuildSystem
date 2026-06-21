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

    /** Writes the configuration to disk. Callers needing atomicity with a preceding mutation use {@link #atomicSave}. */
    public void save() {
        try {
            configuration.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration file: " + file.getName(), e);
        }
    }

    /** Applies {@code mutation} to the configuration and persists it, both under the I/O lock. */
    public void atomicSave(Runnable mutation) {
        synchronized (ioLock) {
            mutation.run();
            save();
        }
    }

    /** Runs {@code work} under the I/O lock — for read/load sequences that must not race a concurrent save. */
    public <T> T locked(Supplier<T> work) {
        synchronized (ioLock) {
            return work.get();
        }
    }
}
