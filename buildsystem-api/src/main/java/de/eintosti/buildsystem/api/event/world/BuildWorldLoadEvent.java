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
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Called when a {@link BuildWorld} is loaded.
 *
 * @since 3.0.0
 */
public class BuildWorldLoadEvent extends BuildWorldEvent implements Cancellable {

    private boolean cancelled = false;

    /**
     * Constructs a new {@link BuildWorldLoadEvent}.
     *
     * @param buildWorld The {@link BuildWorld} that is about to be loaded
     */
    @Internal
    public BuildWorldLoadEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}