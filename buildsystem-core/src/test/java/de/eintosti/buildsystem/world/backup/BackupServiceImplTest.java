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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class BackupServiceImplTest {

    private static final int INTERVAL = 60;

    @TempDir
    Path tempDir;

    @Test
    void autoBackupRefusedAfterTheTick_putsTheTimerBack() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BackupServiceImplTest"));
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().world().backup())
                .thenReturn(new PluginConfig.World.Backup(
                        5,
                        PluginConfig.Storage.Type.LOCAL,
                        "backups",
                        new PluginConfig.World.Backup.AutoBackup(true, false, INTERVAL)));

        WorldDataImpl data = WorldDataSchema.create("held", TestData.NOT_STARTED);
        data.set(WorldDataKey.TIME_SINCE_BACKUP, INTERVAL);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getUniqueId()).thenReturn(UUID.randomUUID());
        when(buildWorld.getName()).thenReturn("held");
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getWorld()).thenReturn(Optional.empty());

        // Free when the tick looks, taken by the time the backup asks for it.
        WorldOperations holder = new WorldOperations(mock(Messages.class), mock(SpawnService.class));
        holder.runExclusively(buildWorld, CompletableFuture::new);
        WorldOperations operations = mock(WorldOperations.class);
        when(operations.runExclusively(any(), any()))
                .thenAnswer(invocation -> holder.runExclusively(buildWorld, CompletableFuture::new));
        WorldStorageImpl worldStorage = mock(WorldStorageImpl.class);
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(buildWorld));
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        when(worldService.operations()).thenReturn(operations);

        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.mainThread()).thenReturn(Runnable::run);

        new BackupServiceImpl(plugin, scheduler, configService, mock(Messages.class), worldService)
                .incrementTimeSinceBackup();

        assertEquals(INTERVAL, data.get(WorldDataKey.TIME_SINCE_BACKUP));
    }
}
