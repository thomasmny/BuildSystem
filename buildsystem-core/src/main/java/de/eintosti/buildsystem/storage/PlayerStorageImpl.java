/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class PlayerStorageImpl implements PlayerStorage {

    protected final BuildSystemPlugin plugin;
    protected final Logger logger;

    private final Map<UUID, BuildPlayer> buildPlayers;

    public PlayerStorageImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.buildPlayers = new HashMap<>();
    }

    public void loadPlayers() {
        try {
            this.buildPlayers.putAll(
                    load().get().stream().collect(Collectors.toMap(BuildPlayer::getUniqueId, Function.identity()))
            );
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Failed to load players from storage: " + e.getMessage());
        }
    }

    @Override
    public BuildPlayer createBuildPlayer(UUID uuid) {
        BuildPlayer buildPlayer = this.buildPlayers.getOrDefault(uuid, new BuildPlayerImpl(uuid, new SettingsImpl()));
        this.buildPlayers.put(uuid, buildPlayer);
        return buildPlayer;
    }

    @Override
    public BuildPlayer createBuildPlayer(Player player) {
        return createBuildPlayer(player.getUniqueId());
    }

    @Override
    public Collection<BuildPlayer> getBuildPlayers() {
        return Collections.unmodifiableCollection(this.buildPlayers.values());
    }

    @Override
    @Nullable
    public BuildPlayer getBuildPlayer(UUID uuid) {
        return this.buildPlayers.get(uuid);
    }

    public BuildPlayer getBuildPlayer(Player player) {
        UUID playerUuid = player.getUniqueId();
        return this.buildPlayers.getOrDefault(playerUuid, createBuildPlayer(playerUuid));
    }
}
