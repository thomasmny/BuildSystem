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
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Called before BuildSystem creates or imports a world.
 *
 * <p>This event is fired before any world directory is created or copied. Cancelling it prevents the world, and any
 * world directory changes, from being created.
 *
 * <p>Because the {@link BuildWorld} does not exist yet, this event exposes the prospective world's properties directly
 * rather than a {@link BuildWorld} instance.
 *
 * @since 4.0.0
 */
@NullMarked
public class BuildWorldCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String worldName;
    private final BuildWorldType type;
    private final @Nullable Builder creator;
    private final boolean importMode;

    private boolean cancelled = false;

    /**
     * Constructs a new {@link BuildWorldCreateEvent}.
     *
     * @param worldName The name of the world that is about to be created
     * @param type The {@link BuildWorldType} of the world that is about to be created
     * @param creator The {@link Builder} creating the world, or {@code null} if not created by a player
     * @param importMode {@code true} if an existing directory is being imported, {@code false} if a new world is
     *     being generated
     */
    @Internal
    public BuildWorldCreateEvent(String worldName, BuildWorldType type, @Nullable Builder creator, boolean importMode) {
        this.worldName = worldName;
        this.type = type;
        this.creator = creator;
        this.importMode = importMode;
    }

    /**
     * Gets the name of the world that is about to be created.
     *
     * @return The name of the world
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Gets the {@link BuildWorldType} of the world that is about to be created.
     *
     * @return The type of the world
     */
    public BuildWorldType getType() {
        return type;
    }

    /**
     * Gets the {@link Builder} creating the world.
     *
     * @return The builder creating the world, or {@code null} if not created by a player
     */
    public @Nullable Builder getCreator() {
        return creator;
    }

    /**
     * Gets whether an existing directory is being imported rather than a new world generated.
     *
     * @return {@code true} if the world is being imported, {@code false} if it is being generated
     */
    public boolean isImport() {
        return importMode;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
