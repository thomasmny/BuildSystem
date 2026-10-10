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
package de.eintosti.buildsystem.world.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.lifecycle.WorldLoader;
import de.eintosti.buildsystem.api.world.lifecycle.WorldTeleporter;
import de.eintosti.buildsystem.api.world.lifecycle.WorldUnloader;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Pins the retention contract of {@link BackupProfileImpl#createBackup()}: archives over {@code maxBackupsPerWorld}
 * are deleted oldest-first by {@link Backup#creationTime()}, and the cap reserves exactly one slot for the backup
 * being created.
 *
 * <p>{@code createBackup()} hops onto the main thread via {@link Bukkit#getScheduler()}; rather than pull in
 * MockBukkit for a single scheduler hop, {@link Bukkit} is mocked statically with a scheduler stub that runs the
 * submitted task immediately, so the whole chain resolves synchronously.
 */
@NullMarked
class BackupProfileImplTest {

    private BuildSystemPlugin plugin;
    private ConfigService configService;
    private BackupStorage backupStorage;
    private BuildWorld buildWorld;
    private MockedStatic<Bukkit> bukkit;
    private final WorldOperations operations = new WorldOperations(mock(Messages.class), mock(SpawnService.class));

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class);
        configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        backupStorage = mock(BackupStorage.class);
        buildWorld = mock(BuildWorld.class);
        lenient().when(buildWorld.getUniqueId()).thenReturn(UUID.randomUUID());
        when(buildWorld.getWorld()).thenReturn(Optional.empty());

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        });
        PluginManager pluginManager = mock(PluginManager.class);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    /** A scheduler that runs "main thread" work inline, so the post-delete events fire within the test. */
    private static TaskScheduler inlineScheduler() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        lenient().when(scheduler.mainThread()).thenReturn(Runnable::run);
        lenient()
                .doAnswer(invocation -> {
                    invocation.getArgument(0, Runnable.class).run();
                    return null;
                })
                .when(scheduler)
                .run(any());
        return scheduler;
    }

    private BackupProfileImpl profile(int maxBackupsPerWorld) {
        when(configService.current().world().backup().maxBackupsPerWorld()).thenReturn(maxBackupsPerWorld);
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.operations()).thenReturn(operations);
        return new BackupProfileImpl(
                plugin,
                inlineScheduler(),
                configService,
                mock(Messages.class),
                worldService,
                () -> backupStorage,
                buildWorld);
    }

    private static Backup backup(long creationTime) {
        Backup backup = mock(Backup.class);
        when(backup.creationTime()).thenReturn(creationTime);
        return backup;
    }

    private void stubListingAndStore(List<Backup> existingBackups) {
        Backup stored = backup(Long.MAX_VALUE);
        when(backupStorage.listBackups(buildWorld)).thenReturn(CompletableFuture.completedFuture(existingBackups));
        when(backupStorage.storeBackup(buildWorld)).thenReturn(CompletableFuture.completedFuture(stored));
        when(backupStorage.deleteBackup(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void atCap_deletesOnlyTheOldest() throws Exception {
        Backup oldest = backup(1_000L);
        Backup middle = backup(2_000L);
        Backup newest = backup(3_000L);
        // Shuffled insertion order: a "delete the first N of the list" implementation would delete the wrong one.
        stubListingAndStore(List.of(newest, oldest, middle));

        profile(3).createBackup().get(5, TimeUnit.SECONDS);

        verify(backupStorage, times(1)).deleteBackup(oldest);
        verify(backupStorage, never()).deleteBackup(middle);
        verify(backupStorage, never()).deleteBackup(newest);
    }

    @Test
    void underCap_deletesNothing() throws Exception {
        Backup older = backup(1_000L);
        Backup newer = backup(2_000L);
        stubListingAndStore(List.of(older, newer));

        profile(3).createBackup().get(5, TimeUnit.SECONDS);

        verify(backupStorage, never()).deleteBackup(any());
    }

    @Test
    void capLoweredBelowExisting_deletesTheTwoOldest() throws Exception {
        Backup oldest = backup(1_000L);
        Backup secondOldest = backup(2_000L);
        Backup secondNewest = backup(3_000L);
        Backup newest = backup(4_000L);
        // Shuffled insertion order: a "delete the first N of the list" implementation would delete the wrong ones.
        stubListingAndStore(List.of(secondNewest, oldest, newest, secondOldest));

        profile(3).createBackup().get(5, TimeUnit.SECONDS);

        verify(backupStorage, times(1)).deleteBackup(oldest);
        verify(backupStorage, times(1)).deleteBackup(secondOldest);
        verify(backupStorage, never()).deleteBackup(secondNewest);
        verify(backupStorage, never()).deleteBackup(newest);
    }

    @Test
    void whileTheWorldIsBusy_backupIsRefusedAndNothingIsStored() {
        stubListingAndStore(List.of());
        BackupProfileImpl profile = profile(3);
        operations.tryBegin(buildWorld);

        assertThrows(ExecutionException.class, () -> profile.createBackup().get(5, TimeUnit.SECONDS));

        verify(backupStorage, never()).storeBackup(any());
    }

    @Test
    void finishedBackup_freesTheWorldAgain() throws Exception {
        stubListingAndStore(List.of());

        profile(3).createBackup().get(5, TimeUnit.SECONDS);

        assertFalse(operations.isBusy(buildWorld));
    }

    /** A loaded world "arena" whose folder is {@code folder}; it unloads only while {@code unloads} is true. */
    private BackupProfileImpl restorableArena(Path folder, Messages messages, boolean unloads) throws IOException {
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("level.dat"), "current");
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("BackupProfileImplTest"));
        AtomicBoolean loaded = new AtomicBoolean(true);
        World arena = mock(World.class);
        when(arena.getWorldFolder()).thenReturn(folder.toFile());
        when(arena.getPlayers()).thenReturn(List.of());
        bukkit.when(() -> Bukkit.getWorld("arena")).thenAnswer(invocation -> loaded.get() ? arena : null);
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(mock(World.class, RETURNS_DEEP_STUBS), arena));

        when(buildWorld.getName()).thenReturn("arena");
        when(buildWorld.getWorld()).thenReturn(Optional.of(arena));
        WorldUnloader unloader = mock(WorldUnloader.class);
        lenient()
                .doAnswer(invocation -> {
                    loaded.set(!unloads);
                    return null;
                })
                .when(unloader)
                .forceUnload(any());
        when(buildWorld.getUnloader()).thenReturn(unloader);
        lenient().when(buildWorld.getLoader()).thenReturn(mock(WorldLoader.class));
        lenient().when(buildWorld.getTeleporter()).thenReturn(mock(WorldTeleporter.class));

        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.operations()).thenReturn(operations);
        return new BackupProfileImpl(
                plugin, inlineScheduler(), configService, messages, worldService, () -> backupStorage, buildWorld);
    }

    private Backup downloadedArchive(String resource) throws IOException {
        Path archive = Files.createTempFile("backup", ".zip");
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/backups/" + resource))) {
            Files.copy(in, archive, StandardCopyOption.REPLACE_EXISTING);
        }
        Backup backup = backup(1L);
        when(backupStorage.downloadBackup(backup)).thenReturn(CompletableFuture.completedFuture(archive.toFile()));
        return backup;
    }

    @Test
    void restoringALocalBackupFromAnEarlierVersion_putsTheWorldBackInPlace(@TempDir Path container) throws Exception {
        Path folder = container.resolve("arena");
        Messages messages = mock(Messages.class);
        when(messages.formatDateTime(anyLong())).thenReturn("now");
        BackupProfileImpl profile = restorableArena(folder, messages, true);
        Backup backup = downloadedArchive("zip4j-local.zip");

        profile.restoreBackup(backup, mock(Player.class)).join();

        assertEquals("level-data", Files.readString(folder.resolve("level.dat")));
        assertFalse(Files.exists(folder.resolve("legacy")));
        verify(buildWorld.getLoader()).load();
        assertFalse(operations.isBusy(buildWorld));
    }

    @Test
    void restoreWhoseUnloadIsRefused_leavesTheWorldAlone(@TempDir Path container) throws Exception {
        Path folder = container.resolve("arena");
        Messages messages = mock(Messages.class);
        BackupProfileImpl profile = restorableArena(folder, messages, false);
        Backup backup = downloadedArchive("zip4j-remote.zip");
        Player player = mock(Player.class);

        profile.restoreBackup(backup, player).join();

        assertEquals("current", Files.readString(folder.resolve("level.dat")));
        verify(messages).sendMessage(eq(player), eq("worlds_world_unload_failed"), any(Placeholders.class));
        assertFalse(operations.isBusy(buildWorld));
    }
}
