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
package de.eintosti.buildsystem.player;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerServiceImpl implements PlayerService {

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private final WorldStorageImpl worldStorage;
    private final PlayerStorageImpl playerStorage;
    private final MaxWorldsResolver maxWorldsResolver;

    private final Set<UUID> buildModePlayers;

    public PlayerServiceImpl(
            BuildSystemPlugin plugin,
            ConfigService configService,
            WorldStorageImpl worldStorage,
            TaskScheduler scheduler,
            EntityStore store) {
        this.plugin = plugin;
        this.configService = configService;
        this.worldStorage = worldStorage;
        this.playerStorage = new PlayerStorageImpl(plugin, scheduler, store);
        this.maxWorldsResolver = new MaxWorldsResolver(plugin.getLogger());
        this.buildModePlayers = ConcurrentHashMap.newKeySet();
    }

    public void init() {
        this.playerStorage.loadPlayers();
    }

    @Override
    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    @Override
    public Set<UUID> getBuildModePlayers() {
        return Collections.unmodifiableSet(buildModePlayers);
    }

    @Override
    public boolean isInBuildMode(Player player) {
        return buildModePlayers.contains(player.getUniqueId());
    }

    @Override
    public boolean enterBuildMode(UUID playerId) {
        return buildModePlayers.add(playerId);
    }

    @Override
    public boolean leaveBuildMode(UUID playerId) {
        return buildModePlayers.remove(playerId);
    }

    /**
     * Puts the player in build mode and snapshots their gamemode and inventory, which {@link #endBuildSession} gives
     * back. Unlike {@link #enterBuildMode(UUID)}, which only flags the player, this is the whole transition.
     *
     * @return {@code false}, leaving the existing snapshot alone, if the player was already in build mode
     */
    public boolean startBuildSession(Player player) {
        if (!enterBuildMode(player.getUniqueId())) {
            return false;
        }
        BuildPlayerImpl.of(playerStorage.getBuildPlayer(player))
                .getCachedValues()
                .saveBuildState(player);
        return true;
    }

    /**
     * Takes the player out of build mode and gives back the gamemode and inventory {@link #startBuildSession} saved.
     *
     * @return {@code false} if the player was not in build mode
     */
    public boolean endBuildSession(Player player) {
        if (!leaveBuildMode(player.getUniqueId())) {
            return false;
        }
        BuildPlayerImpl.of(playerStorage.getBuildPlayer(player))
                .getCachedValues()
                .resetBuildStateIfPresent(player);
        return true;
    }

    @Override
    public boolean canCreateWorld(Player player, Visibility visibility) {
        if (player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            return true;
        }

        int max = getMaxWorlds(player, visibility);
        if (max < 0) {
            max = configuredLimit(visibility);
        }
        if (max < 0) {
            return true;
        }

        return worldStorage.getBuildWorldsCreatedByPlayer(player, visibility).size() < max;
    }

    /**
     * {@return the configured fallback limit for the given visibility, or {@code -1} for unlimited}
     */
    private int configuredLimit(Visibility visibility) {
        PluginConfig.World.Limits limits = configService.current().world().limits();
        return visibility == Visibility.ADDED_PLAYERS ? limits.privateWorlds() : limits.publicWorlds();
    }

    @Override
    public int getMaxWorlds(Player player, Visibility visibility) {
        return maxWorldsResolver.getMaxWorlds(player, visibility);
    }

    public CompletableFuture<Void> save() {
        return this.playerStorage.save(this.playerStorage.getBuildPlayers()).whenComplete((r, e) -> {
            if (e != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
            }
        });
    }
}
