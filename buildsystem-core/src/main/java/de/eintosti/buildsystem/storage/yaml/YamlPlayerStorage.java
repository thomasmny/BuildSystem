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
package de.eintosti.buildsystem.storage.yaml;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.codec.PlayerCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class YamlPlayerStorage extends PlayerStorageImpl {

    private static final String PLAYERS_KEY = "players";

    private final YamlStore store;
    private final FileConfiguration config;
    private final PlayerCodec codec;

    public YamlPlayerStorage(BuildSystemPlugin plugin) {
        super(plugin.getLogger());
        this.store = new YamlStore(plugin.getDataFolder(), "players.yml", plugin.getLogger());
        this.config = store.config();
        this.codec = new PlayerCodec(plugin.getLogger());
    }

    @Override
    public CompletableFuture<Void> save(BuildPlayer buildPlayer) {
        // Serialize on the calling (main) thread; the async block only writes the captured map to disk.
        String playerKey = codec.key(buildPlayer);
        Map<String, Object> serialized = codec.serialize(buildPlayer);
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> config.set(PLAYERS_KEY + "." + playerKey, serialized)));
    }

    @Override
    public CompletableFuture<Void> save(Collection<BuildPlayer> players) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        for (BuildPlayer player : players) {
            serialized.put(codec.key(player), codec.serialize(player));
        }
        return CompletableFuture.runAsync(() -> store.atomicSave(
                () -> serialized.forEach((playerKey, value) -> config.set(PLAYERS_KEY + "." + playerKey, value))));
    }

    @Override
    public CompletableFuture<Collection<BuildPlayer>> load() {
        return CompletableFuture.supplyAsync(() -> store.locked(() -> {
            Collection<BuildPlayer> players = new ArrayList<>();
            for (String playerUuid : loadPlayerKeys()) {
                try {
                    ConfigurationSection section = config.getConfigurationSection(PLAYERS_KEY + "." + playerUuid);
                    if (section == null) {
                        continue;
                    }
                    players.add(codec.deserialize(playerUuid, section));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Skipping player \"" + playerUuid + "\": could not be loaded", e);
                }
            }
            return players;
        }));
    }

    private Set<String> loadPlayerKeys() {
        if (!store.reload()) {
            return Set.of();
        }

        ConfigurationSection section = config.getConfigurationSection(PLAYERS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    @Override
    public CompletableFuture<Void> delete(BuildPlayer buildPlayer) {
        return delete(buildPlayer.getUniqueId().toString());
    }

    @Override
    public CompletableFuture<Void> delete(String playerKey) {
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> config.set(PLAYERS_KEY + "." + playerKey, null)));
    }
}
