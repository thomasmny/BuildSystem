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
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.BukkitWorldFactory;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import io.papermc.lib.PaperLib;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    private final SpawnService spawnService;
    private final TaskScheduler scheduler;

    public WorldRenamer(
            BuildSystemPlugin plugin,
            WorldServiceImpl worldService,
            WorldStorageImpl worldStorage,
            ConfigService configService,
            Messages messages,
            SpawnService spawnService,
            TaskScheduler scheduler) {
        this.plugin = plugin;
        this.worldService = worldService;
        this.worldStorage = worldStorage;
        this.configService = configService;
        this.messages = messages;
        this.spawnService = spawnService;
        this.scheduler = scheduler;
    }

    public void rename(Player player, BuildWorld buildWorld, String newName) {
        player.closeInventory();

        if (worldStorage.worldAndFolderExist(newName)) {
            messages.sendMessage(player, "worlds_world_exists");
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        String oldName = buildWorld.getName();
        if (oldName.equalsIgnoreCase(newName)) {
            messages.sendMessage(player, "worlds_rename_same_name");
            return;
        }

        if (StringCleaner.hasInvalidNameCharacters(
                newName, configService.current().world().invalidCharacters())) {
            messages.sendMessage(player, "worlds_world_creation_invalid_characters");
        }
        String sanitizedNewName =
                StringCleaner.sanitize(newName, configService.current().world().invalidCharacters());
        if (sanitizedNewName.isEmpty()) {
            messages.sendMessage(player, "worlds_world_creation_name_bank");
            return;
        }

        if (Bukkit.getWorld(oldName) == null && !buildWorld.isLoaded()) {
            buildWorld.getLoader().load();
        }

        World oldWorld = Bukkit.getWorld(oldName);
        if (oldWorld == null) {
            messages.sendMessage(player, "worlds_rename_unknown_world");
            return;
        }

        prepareAndMove(player, buildWorld, oldName, sanitizedNewName, oldWorld);
    }

    private void prepareAndMove(
            Player player, BuildWorld buildWorld, String oldName, String sanitizedNewName, World oldWorld) {
        List<@Nullable Player> removedPlayers =
                worldService.removePlayersFromWorld(oldName, "worlds_rename_players_world");
        for (Chunk chunk : oldWorld.getLoadedChunks()) {
            chunk.unload(true);
        }
        Location oldSpawnLocation = oldWorld.getSpawnLocation();
        Bukkit.unloadWorld(oldWorld, true);

        File oldWorldFile = new File(Bukkit.getWorldContainer(), oldName);
        File newWorldFile = new File(Bukkit.getWorldContainer(), sanitizedNewName);
        CompletableFuture.runAsync(
                        () -> {
                            try {
                                FileUtils.copy(oldWorldFile, newWorldFile);
                                FileUtils.deleteDirectory(oldWorldFile);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to rename world directory", e);
                            }
                        },
                        scheduler.background())
                .thenRunAsync(
                        () -> reconstruct(
                                player,
                                buildWorld,
                                oldName,
                                sanitizedNewName,
                                oldWorld,
                                oldSpawnLocation,
                                removedPlayers),
                        scheduler.mainThread());
    }

    private void reconstruct(
            Player player,
            BuildWorld buildWorld,
            String oldName,
            String sanitizedNewName,
            World oldWorld,
            Location oldSpawnLocation,
            List<@Nullable Player> removedPlayers) {
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
        Location spawnLocation = oldSpawnLocation.clone();
        spawnLocation.setWorld(newWorld);

        removedPlayers.stream()
                .filter(Objects::nonNull)
                .forEach(pl -> PaperLib.teleportAsync(pl, spawnLocation.clone().add(0.5, 0, 0.5)));

        Location oldSpawn = spawnService.getSpawn();
        if (oldSpawn != null && Objects.equals(spawnService.getSpawnWorld(), oldWorld)) {
            Location newSpawn = new Location(
                    newWorld,
                    oldSpawn.getX(),
                    oldSpawn.getY(),
                    oldSpawn.getZ(),
                    oldSpawn.getYaw(),
                    oldSpawn.getPitch());
            spawnService.set(newSpawn, sanitizedNewName);
        }

        messages.sendMessage(
                player, "worlds_rename_set", Map.entry("%oldName%", oldName), Map.entry("%newName%", sanitizedNewName));
    }
}
