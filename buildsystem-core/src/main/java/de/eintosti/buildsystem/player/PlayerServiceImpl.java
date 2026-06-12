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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.factory.PlayerStorageFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlayerServiceImpl implements PlayerService {

    @Nullable
    private final BuildSystemPlugin plugin;
    @Nullable
    private final PlayerStorageImpl playerStorage;

    private final Set<UUID> buildModePlayers;
    private final Logger logger;

    public PlayerServiceImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerStorage = new PlayerStorageFactory(plugin).createStorage();
        this.buildModePlayers = new HashSet<>();
    }

    /** Package-private for unit tests — only getMaxWorlds logic is available. */
    PlayerServiceImpl(Logger logger) {
        this.plugin = null;
        this.logger = logger;
        this.playerStorage = null;
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
        boolean showPrivateWorlds = visibility == Visibility.PRIVATE;
        WorldStorageImpl worldStorage = plugin.getWorldService().getWorldStorage();

        int maxWorldAmountConfig = showPrivateWorlds
                ? plugin.getConfigService().current().world().limits().privateWorlds()
                : plugin.getConfigService().current().world().limits().publicWorlds();
        if (maxWorldAmountConfig >= 0 && worldStorage.getBuildWorlds().size() >= maxWorldAmountConfig) {
            return false;
        }

        int maxWorldAmountPlayer = getMaxWorlds(player, showPrivateWorlds ? Visibility.PRIVATE : Visibility.PUBLIC);
        return maxWorldAmountPlayer < 0 || worldStorage.getBuildWorldsCreatedByPlayer(player, visibility).size() < maxWorldAmountPlayer;
    }

    @Override
    public int getMaxWorlds(Player player, Visibility visibility) {
        int max = -1;
        if (player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            return max;
        }

        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String permissionString = permission.getPermission();
            String[] splitPermission = permissionString.split("\\.");

            if (splitPermission.length != 4) {
                continue;
            }

            if (!splitPermission[1].equalsIgnoreCase("create")) {
                continue;
            }

            if (!splitPermission[2].equalsIgnoreCase(visibility.name())) {
                continue;
            }

            String amountString = splitPermission[3];
            if (amountString.equals("*")) {
                return -1;
            }

            try {
                int amount = Integer.parseInt(amountString);
                if (amount > max) {
                    max = amount;
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid max. world amount (must be int)", e);
            }
        }

        return max;
    }

    public CompletableFuture<Void> save() {
        return this.playerStorage
                .save(this.playerStorage.getBuildPlayers())
                .whenComplete((r, e) -> {
                    if (e != null) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
                    }
                });
    }
}
