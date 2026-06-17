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
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.yaml.YamlPlayerStorage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerServiceImpl implements PlayerService {

    private final BuildSystemPlugin plugin;
    private final PlayerStorageImpl playerStorage;
    private final MaxWorldsResolver maxWorldsResolver;

    private final Set<UUID> buildModePlayers;

    public PlayerServiceImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerStorage = new YamlPlayerStorage(plugin);
        this.maxWorldsResolver = new MaxWorldsResolver(plugin.getLogger());
        this.buildModePlayers = new HashSet<>();
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
        return buildModePlayers;
    }

    @Override
    public boolean isInBuildMode(Player player) {
        return buildModePlayers.contains(player.getUniqueId());
    }

    @Override
    public boolean canCreateWorld(Player player, Visibility visibility) {
        boolean showPrivateWorlds = visibility == Visibility.ADDED_PLAYERS;
        WorldStorageImpl worldStorage = plugin.getWorldService().getWorldStorage();

        int maxWorldAmountConfig = showPrivateWorlds
                ? plugin.getConfigService().current().world().limits().privateWorlds()
                : plugin.getConfigService().current().world().limits().publicWorlds();
        if (maxWorldAmountConfig >= 0 && worldStorage.getBuildWorlds().size() >= maxWorldAmountConfig) {
            return false;
        }

        int maxWorldAmountPlayer =
                getMaxWorlds(player, showPrivateWorlds ? Visibility.ADDED_PLAYERS : Visibility.EVERYONE);
        return maxWorldAmountPlayer < 0
                || worldStorage
                                .getBuildWorldsCreatedByPlayer(player, visibility)
                                .size()
                        < maxWorldAmountPlayer;
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
