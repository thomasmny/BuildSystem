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

/**
 * World lifecycle operations: loading, unloading, and teleporting into
 * {@link de.eintosti.buildsystem.api.world.BuildWorld}s.
 *
 * <h2>Async contract</h2>
 *
 * <p>These operations follow an <b>"honest sync"</b> contract: a method's return type reflects whether it actually does
 * asynchronous work. Every lifecycle operation here drives Bukkit's world machinery
 * ({@code WorldCreator.createWorld()}, {@code Bukkit.unloadWorld()}, player teleports), all of which are <b>main-thread
 * only</b>. They therefore run synchronously on the calling thread and return {@code void} — they do not return a
 * {@link java.util.concurrent.CompletableFuture}, because there is nothing to await.
 *
 * <p>This deliberately differs from the genuinely async I/O operations elsewhere in the API
 * ({@code WorldService.deleteWorld(...)}, {@code WorldService.unimportWorld(...)},
 * {@code BackupProfile.restoreBackup(...)}), which return {@code CompletableFuture<Void>} because they perform file or
 * network I/O off the main thread. The rule for the whole API surface: <b>{@code void}/direct-return means synchronous;
 * {@code CompletableFuture} means it really runs asynchronously.</b> Each method documents its threading requirement
 * via an {@code @apiNote}.
 *
 * @since 3.0.0
 */
@NullMarked
package de.eintosti.buildsystem.api.world.lifecycle;

import org.jspecify.annotations.NullMarked;
