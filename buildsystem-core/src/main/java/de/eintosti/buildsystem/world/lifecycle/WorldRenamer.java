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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.BukkitWorldFactory;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import io.papermc.lib.PaperLib;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Orchestrates renaming a {@link BuildWorld}: validates the new name, evicts players, copies the directory asynchronously, then reconstructs the world under the new name.
 */
@NullMarked
public class WorldRenamer {

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;
    private final WorldStorageImpl worldStorage;

    public WorldRenamer(BuildSystemPlugin plugin, WorldServiceImpl worldService, WorldStorageImpl worldStorage) {
        this.plugin = plugin;
        this.worldService = worldService;
        this.worldStorage = worldStorage;
    }

    public void rename(Player player, BuildWorld buildWorld, String newName) {
        player.closeInventory();

        if (worldStorage.worldAndFolderExist(newName)) {
            plugin.getMessages().sendMessage(player, "worlds_world_exists");
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        String oldName = buildWorld.getName();
        if (oldName.equalsIgnoreCase(newName)) {
            plugin.getMessages().sendMessage(player, "worlds_rename_same_name");
            return;
        }

        if (StringCleaner.hasInvalidNameCharacters(
                newName, plugin.getConfigService().current().world().invalidCharacters())) {
            plugin.getMessages().sendMessage(player, "worlds_world_creation_invalid_characters");
        }
        String sanitizedNewName = StringCleaner.sanitize(
                newName, plugin.getConfigService().current().world().invalidCharacters());
        if (sanitizedNewName.isEmpty()) {
            plugin.getMessages().sendMessage(player, "worlds_world_creation_name_bank");
            return;
        }

        if (Bukkit.getWorld(oldName) == null && !buildWorld.isLoaded()) {
            buildWorld.getLoader().load();
        }

        World oldWorld = Bukkit.getWorld(oldName);
        if (oldWorld == null) {
            plugin.getMessages().sendMessage(player, "worlds_rename_unknown_world");
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
        CompletableFuture.runAsync(() -> {
                    try {
                        FileUtils.copy(oldWorldFile, newWorldFile);
                        FileUtils.deleteDirectory(oldWorldFile);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to rename world directory", e);
                    }
                })
                .thenRun(() -> Bukkit.getScheduler()
                        .runTask(
                                plugin,
                                () -> reconstruct(
                                        player,
                                        buildWorld,
                                        oldName,
                                        sanitizedNewName,
                                        oldWorld,
                                        oldSpawnLocation,
                                        removedPlayers)));
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
        worldStorage.save(buildWorld);
        World newWorld = new BukkitWorldFactory(plugin, buildWorld).generate(BukkitWorldFactory.VersionCheck.SKIP);
        Location spawnLocation = oldSpawnLocation;
        spawnLocation.setWorld(newWorld);

        removedPlayers.stream()
                .filter(Objects::nonNull)
                .forEach(pl -> PaperLib.teleportAsync(pl, spawnLocation.clone().add(0.5, 0, 0.5)));

        SpawnService spawnService = plugin.getSpawnService();
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

        plugin.getMessages()
                .sendMessage(
                        player,
                        "worlds_rename_set",
                        Map.entry("%oldName%", oldName),
                        Map.entry("%newName%", sanitizedNewName));
    }
}
