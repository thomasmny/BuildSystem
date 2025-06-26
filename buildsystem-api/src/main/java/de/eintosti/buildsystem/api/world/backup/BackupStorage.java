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
package de.eintosti.buildsystem.api.world.backup;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BackupStorage {

    List<Backup> listBackups(BackupProfile owner, BuildWorld buildWorld);

    void storeBackup(BackupProfile owner, BuildWorld buildWorld, CompletableFuture<Backup> future);

    File downloadBackup(Backup backup);

    void deleteBackup(Backup backup);

    default String getBackupName(BuildWorld buildWorld, long timestamp) {
        return buildWorld.getUniqueId() + "-" + timestamp + ".zip";
    }
}
