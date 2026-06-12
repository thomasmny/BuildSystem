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
 * The BuildSystem API — a programmatic interface for Minecraft (Spigot/Paper) plugins to
 * interact with BuildSystem's world management, player preferences and backup capabilities.
 *
 * <h2>Getting started</h2>
 * <p>Obtain the API instance through Bukkit's {@link org.bukkit.plugin.ServicesManager}:</p>
 * <pre>{@code
 * BuildSystem api = getServer().getServicesManager()
 *         .getRegistration(BuildSystem.class)
 *         .getProvider();
 * }</pre>
 * <p>Alternatively, use the static shorthand
 * {@link de.eintosti.buildsystem.api.BuildSystemProvider#get()}.</p>
 *
 * <h2>Core concepts</h2>
 * <ul>
 *   <li>{@link de.eintosti.buildsystem.api.world.BuildWorld} — a managed world with
 *       metadata (status, visibility, builders, world data).</li>
 *   <li>{@link de.eintosti.buildsystem.api.world.WorldService} — registry and lifecycle
 *       operations for all managed worlds.</li>
 *   <li>{@link de.eintosti.buildsystem.api.player.BuildPlayer} — per-player identity and
 *       settings within BuildSystem.</li>
 *   <li>{@link de.eintosti.buildsystem.api.player.PlayerService} — registry for all known
 *       players.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>Unless a method's documentation explicitly states otherwise, all API calls must be
 * made from the <strong>server main thread</strong>. Methods that perform I/O or network
 * operations return {@link java.util.concurrent.CompletableFuture} and document which
 * thread the future completes on.</p>
 *
 * <h2>Nullability</h2>
 * <p>All packages are annotated {@code @NullMarked}. Return values that may be absent are
 * annotated {@code @Nullable}; parameters accept {@code null} only when annotated.
 * IDE null-analysis will surface misuse at compile time.</p>
 *
 * <h2>Stability</h2>
 * <p>This API is pre-1.0 — signatures may still change before the first stable release.
 * After that release, the deprecation protocol applies: members are never removed without
 * a {@code @Deprecated(forRemoval = true)} cycle spanning at least one major version.</p>
 *
 * @since 3.0.0
 */
package de.eintosti.buildsystem.api;