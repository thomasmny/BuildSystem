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
package de.eintosti.buildsystem.world.lifecycle;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.event.world.BuildWorldRenameEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.lifecycle.SaveBehavior;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.BukkitWorldFactory;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import io.papermc.lib.PaperLib;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Orchestrates renaming a {@link BuildWorld}: validates the new name, evicts players, copies the directory
 * asynchronously, then reconstructs the world under the new name.
 */
@NullMarked
public class WorldRenamer {

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;
    private final WorldStorageImpl worldStorage;
    private final ConfigService configService;
    private final Messages messages;
    private final Prompts prompts;
    private final SpawnService spawnService;
    private final TaskScheduler scheduler;

    public WorldRenamer(
            BuildSystemPlugin plugin,
            WorldServiceImpl worldService,
            WorldStorageImpl worldStorage,
            ConfigService configService,
            Messages messages,
            Prompts prompts,
            SpawnService spawnService,
            TaskScheduler scheduler) {
        this.plugin = plugin;
        this.worldService = worldService;
        this.worldStorage = worldStorage;
        this.configService = configService;
        this.messages = messages;
        this.prompts = prompts;
        this.spawnService = spawnService;
        this.scheduler = scheduler;
    }

    /**
     * Renames a world to what the player typed. A name typed without a namespace keeps the world's current namespace;
     * typing another namespace moves the world's folder there.
     */
    public void rename(Player player, BuildWorld buildWorld, String input) {
        player.closeInventory();

        String oldName = buildWorld.getName();
        String sanitizedNewName = prompts.sanitizeWorldName(player, worldStorage.renamedWorldName(oldName, input));
        if (sanitizedNewName == null) {
            return;
        }

        if (WorldNames.id(oldName).equals(WorldNames.id(sanitizedNewName))) {
            messages.sendMessage(player, "worlds_rename_same_name");
            return;
        }

        if (worldStorage.worldAndFolderExist(sanitizedNewName)) {
            messages.sendMessage(player, "worlds_world_exists");
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        String clash = worldStorage.bukkitNameClash(sanitizedNewName);
        if (clash != null) {
            messages.sendMessage(
                    player,
                    "worlds_world_name_clash",
                    Placeholders.of()
                            .add("%world%", sanitizedNewName)
                            .add("%other%", clash)
                            .build());
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        if (WorldNames.bukkitWorld(oldName) == null && !buildWorld.isLoaded()) {
            buildWorld.getLoader().load();
        }

        World oldWorld = WorldNames.bukkitWorld(oldName);
        if (oldWorld == null) {
            messages.sendMessage(player, "worlds_rename_unknown_world");
            return;
        }

        WorldOperations operations = worldService.operations();
        if (!operations.tryBegin(buildWorld)) {
            messages.sendMessage(player, "worlds_world_busy", Placeholders.of("%world%", oldName));
            return;
        }

        List<Player> removedPlayers = operations.evacuate(oldName, "worlds_rename_players_world");
        Location oldSpawnLocation = oldWorld.getSpawnLocation();
        if (!WorldOperations.tryUnload(buildWorld, SaveBehavior.SAVE)) {
            operations.end(buildWorld);
            messages.sendMessage(player, "worlds_world_unload_failed", Placeholders.of("%world%", oldName));
            return;
        }

        move(player, buildWorld, oldName, sanitizedNewName, oldSpawnLocation, removedPlayers);
    }

    private void move(
            Player player,
            BuildWorld buildWorld,
            String oldName,
            String sanitizedNewName,
            Location oldSpawnLocation,
            List<Player> removedPlayers) {

        File oldWorldFile = FileUtils.worldFolder(oldName);
        File newWorldFile = FileUtils.worldFolder(sanitizedNewName);
        CompletableFuture.runAsync(
                        () -> {
                            try {
                                FileUtils.copy(oldWorldFile, newWorldFile);
                                FileUtils.deleteDirectory(oldWorldFile);
                            } catch (IOException e) {
                                throw new CompletionException("Failed to rename world directory", e);
                            }
                        },
                        scheduler.background())
                .thenRunAsync(
                        () -> reconstruct(
                                player, buildWorld, oldName, sanitizedNewName, oldSpawnLocation, removedPlayers),
                        scheduler.mainThread())
                .exceptionallyAsync(
                        throwable -> {
                            // reconstruct() is skipped when the move fails, so the world keeps its old name on disk and
                            // in storage; tell the player instead of leaving the rename silently half-done.
                            plugin.getLogger()
                                    .log(Level.SEVERE, "Failed to rename world \"" + oldName + "\"", throwable);
                            messages.sendMessage(player, "worlds_rename_error");
                            return null;
                        },
                        scheduler.mainThread())
                .whenComplete((ignored, throwable) -> worldService.operations().end(buildWorld));
    }

    private void reconstruct(
            Player player,
            BuildWorld buildWorld,
            String oldName,
            String sanitizedNewName,
            Location oldSpawnLocation,
            List<Player> removedPlayers) {
        worldStorage.rename(buildWorld, oldName, sanitizedNewName);
        buildWorld.setName(sanitizedNewName);
        Bukkit.getServer()
                .getPluginManager()
                .callEvent(new BuildWorldRenameEvent(buildWorld, oldName, sanitizedNewName));
        worldStorage.save(buildWorld).whenComplete((result, throwable) -> {
            if (throwable != null) {
                plugin.getLogger()
                        .log(
                                Level.SEVERE,
                                "Failed to persist rename of world \"" + oldName + "\" to \"" + sanitizedNewName + "\"",
                                throwable);
            }
        });
        World newWorld = new BukkitWorldFactory(configService, plugin.getLogger(), buildWorld)
                .generate(BukkitWorldFactory.VersionCheck.SKIP);
        buildWorld.getUnloader().manageUnload();
        Location spawnLocation = oldSpawnLocation.clone();
        spawnLocation.setWorld(newWorld);

        removedPlayers.forEach(
                pl -> PaperLib.teleportAsync(pl, spawnLocation.clone().add(0.5, 0, 0.5)));

        spawnService.renameWorld(oldName, sanitizedNewName);

        messages.sendMessage(
                player,
                "worlds_rename_set",
                Placeholders.of()
                        .add("%oldName%", oldName)
                        .add("%newName%", sanitizedNewName)
                        .build());
    }
}
