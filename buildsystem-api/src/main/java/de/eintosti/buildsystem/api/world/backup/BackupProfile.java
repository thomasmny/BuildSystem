/*
 * Copyright (c) 2023-2025, Thomas Meaney
 * All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.eintosti.buildsystem.api.world.backup;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a profile for managing backups of a specific {@link BuildWorld}. This interface defines operations related to listing, creating, restoring, and destroying backups.
 */
@NullMarked
public interface BackupProfile {

    /**
     * Asynchronously populates a list of available {@link Backup}s under this profile.
     *
     * @return Future that will be completed with available backups
     */
    CompletableFuture<List<Backup>> listBackups();

    /**
     * Creates a backup of the {@link BuildWorld}. If the profile is at the maximum backup capacity, the oldest backup will be deleted.
     *
     * @return Future that completes with the created backup.
     */
    CompletableFuture<Backup> createBackup();

    /**
     * Restores a {@link Backup}.
     *
     * @param backup Backup to restore
     * @param player The player restoring the backup
     */
    void restoreBackup(Backup backup, Player player);
}
