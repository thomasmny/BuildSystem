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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

/**
 * The plugin's single scheduling seam. Wraps the Bukkit scheduler (owning the {@link Plugin} handle so collaborators
 * depend on this one injectable instead of the whole {@code BuildSystemPlugin}) and owns the bounded executor that backs
 * all off-main work. Exposing {@link #mainThread()} and {@link #background()} as {@link Executor}s lets
 * {@link java.util.concurrent.CompletableFuture} stages hop threads through the same seam, so there is no longer a mix of
 * raw {@code Bukkit.getScheduler()} calls, ad-hoc {@code mainThreadExecutor} lambdas and the common ForkJoinPool.
 *
 * <p>The background pool is bounded and daemon-threaded; nothing running on it blocks on another background task (chains
 * use non-blocking {@code CompletableFuture} composition), so a single shared pool cannot self-deadlock. Call
 * {@link #shutdown()} once on plugin disable, after the final saves have completed.
 */
@NullMarked
public final class TaskScheduler {

    private final Plugin plugin;
    private final ExecutorService background;

    public TaskScheduler(Plugin plugin) {
        this.plugin = plugin;
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
        AtomicInteger counter = new AtomicInteger();
        this.background = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "BuildSystem-Worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public BukkitTask run(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    public BukkitTask runLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public BukkitTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public BukkitTask runAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public BukkitTask runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    /**
     * {@return an {@link Executor} that runs each task on the server main thread} For marshalling a
     * {@link java.util.concurrent.CompletableFuture} stage back onto the main thread, e.g.
     * {@code future.thenAcceptAsync(consumer, scheduler.mainThread())}.
     */
    public Executor mainThread() {
        return this::run;
    }

    /**
     * {@return the shared bounded executor for off-main work} Used as the explicit executor for
     * {@code CompletableFuture.supplyAsync/runAsync} so storage I/O and backups share one observable, bounded pool
     * instead of the unbounded common ForkJoinPool.
     */
    public Executor background() {
        return background;
    }

    /** Shuts the background executor down; call once on plugin disable, after the final saves have completed. */
    public void shutdown() {
        background.shutdown();
    }
}
