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
package de.eintosti.buildsystem.storage.yaml;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.LogoutLocation;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldDisplay;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldFilter;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldSort;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocationImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.world.navigator.settings.WorldDisplayImpl;
import de.eintosti.buildsystem.world.navigator.settings.WorldFilterImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlPlayerStorage extends PlayerStorageImpl {

    private static final String PLAYERS_KEY = "players";

    private final File file;
    private final FileConfiguration config;

    public YamlPlayerStorage(BuildSystemPlugin plugin) {
        super(plugin);
        this.file = new File(plugin.getDataFolder(), "players.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public CompletableFuture<Void> save(BuildPlayer buildPlayer) {
        return CompletableFuture.runAsync(() -> {
            config.set(PLAYERS_KEY + "." + buildPlayer.getUniqueId(), serializePlayer(buildPlayer));
            saveFile();
        });
    }

    @Override
    public CompletableFuture<Void> save(Collection<BuildPlayer> players) {
        return CompletableFuture.runAsync(() -> {
            players.forEach(player -> config.set(PLAYERS_KEY + "." + player.getUniqueId(), serializePlayer(player)));
            saveFile();
        });
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save players.yml file", e);
        }
    }

    public Map<String, Object> serializePlayer(BuildPlayer player) {
        Map<String, Object> serialized = new HashMap<>();

        serialized.put("settings", serializeSettings(player.getSettings()));
        if (player.getLogoutLocation() != null) {
            serialized.put("logout-location", player.getLogoutLocation().toString());
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
        return CompletableFuture.supplyAsync(() ->
                loadPlayerKeys().stream()
                        .map(this::loadPlayer)
                        .collect(Collectors.toCollection(ArrayList::new))
        );
    }

    private Set<String> loadPlayerKeys() {
        if (!file.exists()) {
            config.options().copyDefaults(true);
            saveFile();
            return Set.of();
        }

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, "Could not load players.yml file", e);
        }

        ConfigurationSection section = config.getConfigurationSection(PLAYERS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    private BuildPlayer loadPlayer(String playerUuid) {
        final String path = PLAYERS_KEY + "." + playerUuid;

        UUID uuid = UUID.fromString(playerUuid);
        Settings settings = loadSettings(config, path + ".settings");

        BuildPlayer buildPlayer = new BuildPlayerImpl(uuid, settings);
        buildPlayer.setLogoutLocation(loadLogoutLocation(config, "players." + playerUuid + ".logout-location"));
        return buildPlayer;
    }

    @Nullable
    private LogoutLocation loadLogoutLocation(FileConfiguration configuration, String pathPrefix) {
        String location = configuration.getString(pathPrefix);
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        String[] parts = location.split(":");
        if (parts.length != 6) {
            return null;
        }

        String worldName = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        return new LogoutLocationImpl(worldName, x, y, z, yaw, pitch);
    }

    private SettingsImpl loadSettings(FileConfiguration configuration, String pathPrefix) {
        NavigatorType navigatorType = NavigatorType.valueOf(configuration.getString(pathPrefix + ".type"));
        DesignColor glassColor = DesignColor.matchColor(configuration.getString(pathPrefix + ".glass"));
        WorldDisplay worldDisplay = loadWorldDisplay(configuration, pathPrefix + ".world-display");
        boolean clearInventory = configuration.getBoolean(pathPrefix + ".clear-inventory", false);
        boolean disableInteract = configuration.getBoolean(pathPrefix + ".disable-interact", false);
        boolean hidePlayers = configuration.getBoolean(pathPrefix + ".hide-players", false);
        boolean instantPlaceSigns = configuration.getBoolean(pathPrefix + ".instant-place-signs", false);
        boolean keepNavigator = configuration.getBoolean(pathPrefix + ".keep-navigator", false);
        boolean nightVision = configuration.getBoolean(pathPrefix + ".nightvision", false);
        boolean noClip = configuration.getBoolean(pathPrefix + ".no-clip", false);
        boolean placePlants = configuration.getBoolean(pathPrefix + ".place-plants", false);
        boolean scoreboard = configuration.getBoolean(pathPrefix + ".scoreboard", true);
        boolean slabBreaking = configuration.getBoolean(pathPrefix + ".slab-breaking", false);
        boolean spawnTeleport = configuration.getBoolean(pathPrefix + ".spawn-teleport", true);
        boolean trapDoor = configuration.getBoolean(pathPrefix + ".trapdoor", false);

        return new SettingsImpl(
                navigatorType, glassColor, worldDisplay, clearInventory, disableInteract, hidePlayers, instantPlaceSigns,
                keepNavigator, nightVision, noClip, placePlants, scoreboard, slabBreaking, spawnTeleport, trapDoor
        );
    }

    private WorldDisplay loadWorldDisplay(FileConfiguration configuration, String pathPrefix) {
        WorldSort worldSort = WorldSort.matchWorldSort(configuration.getString(pathPrefix + ".sort", WorldSort.NEWEST_FIRST.name()));
        WorldFilter.Mode filterMode = WorldFilterImpl.Mode.valueOf(configuration.getString(pathPrefix + ".filter.mode", WorldFilter.Mode.NONE.name()));
        String filterText = configuration.getString(pathPrefix + ".filter.text", "");
        return new WorldDisplayImpl(worldSort, new WorldFilterImpl(filterMode, filterText));
    }

    @Override
    public CompletableFuture<Void> delete(BuildPlayer buildPlayer) {
        return delete(buildPlayer.getUniqueId().toString());
    }

    @Override
    public CompletableFuture<Void> delete(String playerKey) {
        return CompletableFuture.runAsync(() -> {
            config.set(PLAYERS_KEY + "." + playerKey, null);
            saveFile();
        });
    }
}