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
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.WorldDisplay;
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.codec.LogoutLocationCodec;
import de.eintosti.buildsystem.world.display.WorldDisplayImpl;
import de.eintosti.buildsystem.world.display.WorldFilterImpl;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlPlayerStorage extends PlayerStorageImpl {

    private static final String PLAYERS_KEY = "players";

    private final YamlStore store;
    private final FileConfiguration config;

    public YamlPlayerStorage(BuildSystemPlugin plugin) {
        super(plugin.getLogger());
        this.store = new YamlStore(plugin.getDataFolder(), "players.yml", plugin.getLogger());
        this.config = store.config();
    }

    @Override
    public CompletableFuture<Void> save(BuildPlayer buildPlayer) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() -> config.set(
                PLAYERS_KEY + "." + buildPlayer.getUniqueId(), serializePlayer(BuildPlayerImpl.of(buildPlayer)))));
    }

    @Override
    public CompletableFuture<Void> save(Collection<BuildPlayer> players) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() -> players.forEach(player ->
                config.set(PLAYERS_KEY + "." + player.getUniqueId(), serializePlayer(BuildPlayerImpl.of(player))))));
    }

    public Map<String, Object> serializePlayer(BuildPlayerImpl player) {
        Map<String, Object> serialized = new HashMap<>();

        serialized.put("settings", serializeSettings(player.getSettings()));
        if (player.getLogoutLocation() != null) {
            serialized.put("logout-location", LogoutLocationCodec.format(player.getLogoutLocation()));
        }

        return serialized;
    }

    public Map<String, Object> serializeSettings(Settings settings) {
        Map<String, Object> serialized = new HashMap<>();

        serialized.put("type", settings.getNavigatorType().toString());
        serialized.put("glass", settings.getDesignColor().toString());
        serialized.put("world-display", serializeWorldDisplay(settings.getWorldDisplay()));
        serialized.put("slab-breaking", settings.isSlabBreaking());
        serialized.put("no-clip", settings.isNoClip());
        serialized.put("trapdoor", settings.isOpenTrapDoors());
        serialized.put("nightvision", settings.isNightVision());
        serialized.put("scoreboard", settings.isScoreboard());
        serialized.put("keep-navigator", settings.isKeepNavigator());
        serialized.put("disable-interact", settings.isDisableInteract());
        serialized.put("spawn-teleport", settings.isSpawnTeleport());
        serialized.put("clear-inventory", settings.isClearInventory());
        serialized.put("instant-place-signs", settings.isInstantPlaceSigns());
        serialized.put("hide-players", settings.isHidePlayers());
        serialized.put("place-plants", settings.isPlacePlants());

        return serialized;
    }

    public Map<String, Object> serializeWorldDisplay(WorldDisplay worldDisplay) {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("sort", worldDisplay.getWorldSort().toString());
        serialized.put("filter", serializeFilter(worldDisplay.getWorldFilter()));
        return serialized;
    }

    public Map<String, Object> serializeFilter(WorldFilter filter) {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("mode", filter.getMode().toString());
        serialized.put("text", filter.getText());
        return serialized;
    }

    @Override
    public CompletableFuture<Collection<BuildPlayer>> load() {
        return CompletableFuture.supplyAsync(() -> store.locked(() -> {
            Collection<BuildPlayer> players = new ArrayList<>();
            for (String playerUuid : loadPlayerKeys()) {
                try {
                    players.add(loadPlayer(playerUuid));
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

    private BuildPlayerImpl loadPlayer(String playerUuid) {
        final String path = PLAYERS_KEY + "." + playerUuid;

        UUID uuid = UUID.fromString(playerUuid);
        Settings settings = loadSettings(config, path + ".settings");

        BuildPlayerImpl buildPlayer = new BuildPlayerImpl(uuid, settings);
        buildPlayer.setLogoutLocation(
                LogoutLocationCodec.parse(config.getString("players." + playerUuid + ".logout-location")));
        return buildPlayer;
    }

    private NavigatorType parseNavigatorType(@Nullable String raw) {
        if (raw == null) {
            return NavigatorType.OLD;
        }
        try {
            return NavigatorType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown navigator type \"" + raw + "\". Defaulting to OLD.");
            return NavigatorType.OLD;
        }
    }

    @Contract("_, _ -> new")
    private SettingsImpl loadSettings(FileConfiguration configuration, String pathPrefix) {
        return SettingsImpl.builder()
                .navigatorType(parseNavigatorType(configuration.getString(pathPrefix + ".type")))
                .designColor(DesignColor.matchColor(configuration.getString(pathPrefix + ".glass")))
                .worldDisplay(loadWorldDisplay(configuration, pathPrefix + ".world-display"))
                .clearInventory(configuration.getBoolean(pathPrefix + ".clear-inventory", false))
                .disableInteract(configuration.getBoolean(pathPrefix + ".disable-interact", false))
                .hidePlayers(configuration.getBoolean(pathPrefix + ".hide-players", false))
                .instantPlaceSigns(configuration.getBoolean(pathPrefix + ".instant-place-signs", false))
                .keepNavigator(configuration.getBoolean(pathPrefix + ".keep-navigator", false))
                .nightVision(configuration.getBoolean(pathPrefix + ".nightvision", false))
                .noClip(configuration.getBoolean(pathPrefix + ".no-clip", false))
                .placePlants(configuration.getBoolean(pathPrefix + ".place-plants", false))
                .scoreboard(configuration.getBoolean(pathPrefix + ".scoreboard", true))
                .slabBreaking(configuration.getBoolean(pathPrefix + ".slab-breaking", false))
                .spawnTeleport(configuration.getBoolean(pathPrefix + ".spawn-teleport", true))
                .openTrapDoors(configuration.getBoolean(pathPrefix + ".trapdoor", false))
                .build();
    }

    @Contract("_, _ -> new")
    private WorldDisplay loadWorldDisplay(FileConfiguration configuration, String pathPrefix) {
        WorldSort worldSort =
                WorldSort.matchWorldSort(configuration.getString(pathPrefix + ".sort", WorldSort.NEWEST_FIRST.name()));
        WorldFilter.Mode filterMode = WorldFilterImpl.Mode.valueOf(
                configuration.getString(pathPrefix + ".filter.mode", WorldFilter.Mode.NONE.name()));
        String filterText = configuration.getString(pathPrefix + ".filter.text", "");
        return new WorldDisplayImpl(worldSort, new WorldFilterImpl(filterMode, filterText));
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
