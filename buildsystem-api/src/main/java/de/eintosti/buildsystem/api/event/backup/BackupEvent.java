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
package de.eintosti.buildsystem.api.event.backup;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a {@link Backup} related event.
 *
 * @since 3.0.0
 */
@NullMarked
public abstract class BackupEvent extends Event {

    private final BuildWorld buildWorld;
    private final Backup backup;

    /**
     * Constructs a new {@link BackupEvent}.
     *
     * @param buildWorld The world the backup belongs to
     * @param backup The backup involved in this event
     */
    @Internal
    protected BackupEvent(BuildWorld buildWorld, Backup backup) {
        this.buildWorld = buildWorld;
        this.backup = backup;
    }

    /**
     * Gets the world the backup belongs to.
     *
     * @return The world the backup belongs to
     */
    public BuildWorld getBuildWorld() {
        return buildWorld;
    }

    /**
     * Gets the backup involved in this event.
     *
     * @return The backup involved in this event
     */
    public Backup getBackup() {
        return backup;
    }
}
