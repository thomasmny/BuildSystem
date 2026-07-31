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
package de.eintosti.buildsystem.util;

import java.lang.reflect.Method;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Saves a world and waits for its chunks to reach disk.
 *
 * <p>{@link World#save()} returns while the chunk writer is still working, so a backup or export reading the world
 * folder straight afterwards can pack half-written region files. Paper's {@code save(boolean flush)} waits for the
 * writer to drain; it is not in the Spigot API this module compiles against, so it is resolved reflectively. Spigot
 * itself writes chunks synchronously, so the plain save it falls back to is already flushed there.
 */
@NullMarked
public final class WorldFlush {

    private static final @Nullable Method SAVE_WITH_FLUSH = resolveSaveWithFlush();

    private WorldFlush() {}

    /**
     * Saves {@code world}, blocking until its chunks are written where the platform allows it.
     */
    public static void saveAndFlush(World world) {
        if (SAVE_WITH_FLUSH != null) {
            try {
                SAVE_WITH_FLUSH.invoke(world, true);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall through to the unflushed save rather than failing the export outright.
            }
        }
        world.save();
    }

    private static @Nullable Method resolveSaveWithFlush() {
        try {
            return World.class.getMethod("save", boolean.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
