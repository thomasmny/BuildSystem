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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The YAML file plumbing — a single backing file, its loaded {@link FileConfiguration}, and the I/O lock that
 * serializes access to both — held by each storage rather than inherited. Composing this collapses the parallel
 * plumbing that single inheritance previously forced the cache-backed storages to duplicate (each re-implementing the
 * {@code file}/{@code config}/{@code ioLock}/{@code save} fields and methods because they already extend a cache base).
 *
 * <p>The cache-backed storages mutate and persist off the main thread, so writes go through {@link #atomicSave} and
 * read/load sequences through {@link #locked}; both hold the same lock, so the non-thread-safe configuration is never
 * mutated or written by two tasks at once.
 *
 * <p>A file that exists but cannot be read is never written over: the configuration then holds little or nothing of it,
 * so saving would erase every entry not in memory. Saves are refused until the file loads cleanly again.
 */
@NullMarked
public final class YamlStore {

    private final File file;
    private final FileConfiguration configuration;
    private final Logger logger;
    private final Object ioLock = new Object();

    private volatile boolean loadFailed;
    private volatile boolean refusalLogged;

    public YamlStore(File dataFolder, String fileName, Logger logger) {
        this.file = new File(dataFolder, fileName);
        this.configuration = new YamlConfiguration();
        this.logger = logger;
        if (file.exists()) {
            load();
        }
    }

    public FileConfiguration config() {
        return configuration;
    }

    public String fileName() {
        return file.getName();
    }

    public boolean exists() {
        return file.exists();
    }

    /**
     * {@return whether the last {@link #reload} found the file but could not read it}
     */
    public boolean loadFailed() {
        return loadFailed;
    }

    /**
     * Re-reads the backing file from disk, creating an empty one when it does not yet exist.
     *
     * @return {@code true} if the file existed and was read, {@code false} if it was just created or could not be read
     */
    public boolean reload() {
        if (!file.exists()) {
            loadFailed = false;
            configuration.options().copyDefaults(true);
            save();
            return false;
        }
        return load();
    }

    private boolean load() {
        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            // A node that cannot be deserialized, such as an item another server software saved, throws a
            // YAMLException rather than an InvalidConfigurationException.
            if (!loadFailed) {
                loadFailed = true;
                refusalLogged = false;
                String copy = keepBrokenCopy();
                logger.log(
                        Level.SEVERE,
                        ("Failed to load %s%s. Nothing in it is saved until it loads again: fix the file or restore a"
                                        + " backup, then restart the server.")
                                .formatted(file.getName(), copy == null ? "" : " (a copy was kept as " + copy + ")"),
                        e);
            }
            return false;
        }
        loadFailed = false;
        return true;
    }

    /**
     * Copies the file that failed to load to a sibling {@code <name>.broken-<timestamp>}.
     *
     * @return The copy's name, or {@code null} if it could not be made
     */
    private @Nullable String keepBrokenCopy() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File copy = new File(file.getParentFile(), file.getName() + ".broken-" + timestamp);
        try {
            Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return copy.getName();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to copy " + file.getName() + " to " + copy.getName(), e);
            return null;
        }
    }

    /**
     * Writes the configuration to disk. Callers needing this synchronized with a preceding mutation use
     * {@link #atomicSave}.
     *
     * <p>Writes to a sibling temp file first and moves it onto {@code file}, so a crash or power loss mid-write
     * never leaves a truncated file in place — {@link #reload} either reads the previous complete file or the new
     * complete one, never a partial one.
     *
     * @return Whether the file was written. A failure is logged, and so is the first save refused because the file
     *     failed to load.
     */
    public boolean save() {
        if (loadFailed) {
            if (!refusalLogged) {
                refusalLogged = true;
                logger.severe("Not saving %s, since it failed to load. Changes made meanwhile are not kept."
                        .formatted(file.getName()));
            }
            return false;
        }

        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            configuration.save(temp);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration file: %s".formatted(file.getName()), e);
            deleteQuietly(temp);
            return false;
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
                return false;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration file: %s".formatted(file.getName()), e);
            deleteQuietly(temp);
            return false;
        }
        return true;
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
     *
     * @return Whether the file was written
     */
    public boolean atomicSave(Runnable mutation) {
        synchronized (ioLock) {
            mutation.run();
            return save();
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
