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
package de.eintosti.buildsystem.world.creation;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Coordinates bulk imports of worlds, running them staggered over a configured delay so the server is not stalled.
 */
@NullMarked
public class WorldImportCoordinator {

    private final AtomicBoolean importingAllWorlds = new AtomicBoolean(false);

    private final BuildSystemPlugin plugin;
    private final Logger logger;
    private final WorldServiceImpl worldService;
    private final WorldStorageImpl worldStorage;
    private final ConfigService configService;
    private final Messages messages;

    public WorldImportCoordinator(
            BuildSystemPlugin plugin,
            WorldServiceImpl worldService,
            WorldStorageImpl worldStorage,
            ConfigService configService,
            Messages messages) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.worldService = worldService;
        this.worldStorage = worldStorage;
        this.configService = configService;
        this.messages = messages;
    }

    public void importWorlds(Player player, String[] worldList, Generator generator, @Nullable Builder creator) {
        int delay = configService.current().world().importAllDelay();
        messages.sendMessage(
                player, "worlds_importall_started", Map.entry("%amount%", String.valueOf(worldList.length)));
        messages.sendMessage(player, "worlds_importall_delay", Map.entry("%delay%", String.valueOf(delay)));

        if (!importingAllWorlds.compareAndSet(false, true)) {
            // The subcommand checks isImportingAllWorlds() first, but that is check-then-act; claim it atomically so
            // two bulk imports cannot overlap and clear each other's flag.
            messages.sendMessage(player, "worlds_importall_already_started");
            return;
        }
        BulkImportListener listener = new BulkImportListener() {
            @Override
            public void skippedExisting(String worldName) {
                messages.sendMessage(
                        player, "worlds_importall_world_already_imported", Map.entry("%world%", worldName));
            }

            @Override
            public void invalidName(String worldName, String invalidChar) {
                messages.sendMessage(
                        player,
                        "worlds_importall_invalid_character",
                        Map.entry("%world%", worldName),
                        Map.entry("%char%", invalidChar));
            }

            @Override
            public void imported(String worldName) {
                messages.sendMessage(player, "worlds_importall_world_imported", Map.entry("%world%", worldName));
            }

            @Override
            public void failed(String worldName) {
                messages.sendMessage(player, "worlds_importall_world_failed", Map.entry("%world%", worldName));
            }
        };
        importStaggered(
                        worldList,
                        listener,
                        worldName -> worldService.importWorld(
                                player, worldName, creator, BuildWorldType.IMPORTED, generator, "void", false))
                .thenRun(() -> messages.sendMessage(player, "worlds_importall_finished"));
    }

    public boolean isImportingAllWorlds() {
        return importingAllWorlds.get();
    }

    public CompletableFuture<Integer> importWorlds() {
        if (!importingAllWorlds.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A bulk import is already in progress"));
        }

        String[] directories = scanImportableDirectories();
        if (directories.length == 0) {
            importingAllWorlds.set(false);
            return CompletableFuture.completedFuture(0);
        }

        // The API returns only a count, so without this every skipped or failed world would vanish without a trace.
        BulkImportListener listener = new BulkImportListener() {
            @Override
            public void skippedExisting(String worldName) {
                logger.info("Bulk import: skipping \"" + worldName + "\", already imported.");
            }

            @Override
            public void invalidName(String worldName, String invalidChar) {
                logger.warning("Bulk import: skipping \"" + worldName + "\", name contains '" + invalidChar + "'.");
            }

            @Override
            public void failed(String worldName) {
                logger.warning("Bulk import: failed to import \"" + worldName + "\".");
            }
        };
        return importStaggered(
                directories,
                listener,
                worldName -> worldService.importWorld(worldName).build() != null);
    }

    /**
     * Per-world progress callbacks for a staggered bulk import. Every outcome has a callback, so a world that is
     * skipped or fails is reported rather than silently reducing the final count.
     */
    private interface BulkImportListener {

        default void skippedExisting(String worldName) {}

        default void invalidName(String worldName, String invalidChar) {}

        default void imported(String worldName) {}

        default void failed(String worldName) {}
    }

    /**
     * Imports the given worlds one per configured delay interval, skipping names that are already imported or contain
     * invalid characters. Clears {@link #importingAllWorlds} and completes with the import count once all names have
     * been processed.
     */
    private CompletableFuture<Integer> importStaggered(
            String[] worldNames, BulkImportListener listener, Predicate<String> importer) {
        int delay = configService.current().world().importAllDelay();
        String invalidCharacters = configService.current().world().invalidCharacters();

        CompletableFuture<Integer> result = new CompletableFuture<>();
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger imported = new AtomicInteger(0);

        new BukkitRunnable() {
            @Override
            public void run() {
                int i = index.getAndIncrement();
                if (i >= worldNames.length) {
                    finish();
                    return;
                }

                String worldName = worldNames[i];
                try {
                    importOne(worldName);
                } catch (RuntimeException e) {
                    // One world that blows up must not strand the run: Bukkit keeps a repeating task alive after an
                    // exception, so without this the flag stays set and no bulk import is ever permitted again.
                    logger.log(Level.SEVERE, "Failed to import world \"" + worldName + "\"", e);
                    listener.failed(worldName);
                }
            }

            private void importOne(String worldName) {
                if (worldStorage.worldExists(worldName)) {
                    listener.skippedExisting(worldName);
                    return;
                }

                String invalidChar = StringCleaner.firstInvalidChar(worldName, invalidCharacters);
                if (invalidChar != null) {
                    listener.invalidName(worldName, invalidChar);
                    return;
                }

                if (importer.test(worldName)) {
                    imported.incrementAndGet();
                    listener.imported(worldName);
                } else {
                    listener.failed(worldName);
                }
            }

            private void finish() {
                this.cancel();
                importingAllWorlds.set(false);
                result.complete(imported.get());
            }
        }.runTaskTimer(plugin, 0, 20L * delay);

        return result;
    }

    private String[] scanImportableDirectories() {
        String[] directories = FileUtils.worldDimensionsRoot()
                .list((dir, name) ->
                        FileUtils.isWorldDirectory(new File(dir, name)) && !worldStorage.worldExists(name));
        return directories != null ? directories : new String[0];
    }
}
