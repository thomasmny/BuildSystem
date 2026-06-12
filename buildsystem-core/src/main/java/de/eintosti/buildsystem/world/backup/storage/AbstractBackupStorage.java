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
package de.eintosti.buildsystem.world.backup.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractBackupStorage implements BackupStorage {

    @Nullable
    protected final BuildSystemPlugin plugin;
    protected final Logger logger;
    private final Executor executor;

    protected AbstractBackupStorage(BuildSystemPlugin plugin, Executor executor) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.executor = executor;
    }

    AbstractBackupStorage(Logger logger, Executor executor) {
        this.plugin = null;
        this.logger = logger;
        this.executor = executor;
    }

    protected static String backupName(long timestamp) {
        return timestamp + ".zip";
    }

    protected void logDuration(BuildWorld buildWorld, long startTimestamp) {
        logger.info("Backed up world '%s'. Took %sms".formatted(
                buildWorld.getName(), System.currentTimeMillis() - startTimestamp
        ));
    }

    /**
     * Runs {@code supplier} on the backup executor; wraps IOException in RuntimeException.
     */
    protected <T> CompletableFuture<T> supply(String operation, IoSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (IOException e) {
                onIoFailure();
                throw new RuntimeException("Failed to " + operation, e);
            }
        }, executor);
    }

    /**
     * Runs {@code task} on the backup executor; wraps IOException in RuntimeException.
     */
    protected CompletableFuture<Void> run(String operation, IoRunnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (IOException e) {
                onIoFailure();
                throw new RuntimeException("Failed to " + operation, e);
            }
        }, executor);
    }

    /**
     * Called on any IOException caught by the template scaffolding. Override to perform cleanup (e.g. SFTP subclass calls {@code disconnectAll()}).
     */
    protected void onIoFailure() {
    }

    @Override
    public final CompletableFuture<List<Backup>> listBackups(BuildWorld buildWorld) {
        return supply("list backups for " + buildWorld.getName(), () -> {
            List<Backup> backups = doListBackups(buildWorld);
            backups.sort(Comparator.comparingLong(Backup::creationTime).reversed());
            return backups;
        });
    }

    @Override
    public final CompletableFuture<Void> deleteBackup(Backup backup) {
        return run("delete backup " + backup.key(), () -> doDeleteBackup(backup));
    }

    /**
     * Returns a mutable list of backups; ordering not required (base class sorts).
     */
    protected abstract List<Backup> doListBackups(BuildWorld buildWorld) throws IOException;

    @Override
    public abstract CompletableFuture<Backup> storeBackup(BuildWorld buildWorld);

    @Override
    public abstract CompletableFuture<File> downloadBackup(Backup backup);

    protected abstract void doDeleteBackup(Backup backup) throws IOException;

    @FunctionalInterface
    protected interface IoSupplier<T> {

        T get() throws IOException;
    }

    @FunctionalInterface
    protected interface IoRunnable {

        void run() throws IOException;
    }
}
