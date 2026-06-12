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
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Called after a {@link Backup} of a {@link BuildWorld} has been created and stored.
 *
 * @since 3.0.0
 */
@NullMarked
public class BackupCreatedEvent extends BackupEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /**
     * Constructs a new {@link BackupCreatedEvent}.
     *
     * @param buildWorld The world the backup belongs to
     * @param backup The backup involved in this event
     */
    @Internal
    public BackupCreatedEvent(BuildWorld buildWorld, Backup backup) {
        super(buildWorld, backup);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets the handler list for this event.
     *
     * @return The handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
