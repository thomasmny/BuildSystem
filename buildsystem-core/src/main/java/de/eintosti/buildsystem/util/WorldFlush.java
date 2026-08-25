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
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Saves a world and holds the chunk writer off while its folder is read.
 *
 * <p>{@link World#save()} returns while the chunk writer is still working, so a backup or export reading the world
 * folder straight afterwards can pack half-written region files. Paper's {@code save(boolean flush)} waits for the
 * writer to drain; it is not in the Spigot API this module compiles against, so it is resolved reflectively. Spigot
 * itself writes chunks synchronously, so the plain save it falls back to is already flushed there.
 *
 * <p>Flushing once is not enough on its own. Zipping a world takes seconds, and the autosave and chunk-unload passes
 * keep rewriting region files throughout. A file rewritten between its header and its sectors being read is packed
 * with the two disagreeing, which only surfaces when that archive is restored, as a corrupt regionfile header. So
 * {@link #saveAndPauseWrites(World)} turns autosave off for the length of the read: the server skips both passes for
 * a world marked no-save, and the save itself is unaffected, since it clears the flag while it runs.
 */
@NullMarked
public final class WorldFlush {

    private static final @Nullable Method SAVE_WITH_FLUSH = resolveSaveWithFlush();

    /**
     * The worlds currently being read, keyed by identity so a world that is unloaded and loaded again starts over
     * rather than inheriting a pause nobody will lift. Read and written on the main thread only.
     */
    private static final Map<World, Pause> PAUSED = new WeakHashMap<>();

    private WorldFlush() {}

    /**
     * Saves {@code world} and stops anything else writing to its folder until {@link #resumeWrites(World)}.
     *
     * <p>Pauses are counted, so a backup and a download overlapping do not hand the writer back while the other is
     * still reading. Main thread only.
     */
    public static void saveAndPauseWrites(World world) {
        PAUSED.compute(
                world,
                (_, pause) -> pause == null
                        ? new Pause(1, world.isAutoSave())
                        : new Pause(pause.depth() + 1, pause.previousAutoSave()));

        world.setAutoSave(false);
        saveAndFlush(world);
    }

    /**
     * Hands {@code world} back to the chunk writer once the last pause has been lifted, as whoever else had a say
     * over its autosave left it. Main thread only.
     */
    public static void resumeWrites(World world) {
        Pause pause = PAUSED.get(world);
        if (pause == null) {
            return;
        }

        if (pause.depth() > 1) {
            PAUSED.put(world, new Pause(pause.depth() - 1, pause.previousAutoSave()));
            return;
        }

        PAUSED.remove(world);
        world.setAutoSave(pause.previousAutoSave());
    }

    /**
     * @param depth            How many reads of this world's folder are in progress
     * @param previousAutoSave Whether the world saved by itself before the first of them
     */
    private record Pause(int depth, boolean previousAutoSave) {}

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
