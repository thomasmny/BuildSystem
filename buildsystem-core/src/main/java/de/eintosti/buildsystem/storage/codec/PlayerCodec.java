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
package de.eintosti.buildsystem.storage.codec;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.WorldDisplay;
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.world.display.WorldDisplayImpl;
import de.eintosti.buildsystem.world.display.WorldFilterImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Codec} for {@link BuildPlayer}s, mapping a player's persistent settings and last logout location to and from
 * the {@code players.<uuid>} section. The leaf key strings are declared once and shared between serialization and
 * deserialization so the two paths cannot drift apart.
 */
@NullMarked
public final class PlayerCodec implements Codec<BuildPlayer> {

    private static final String SETTINGS = "settings";
    private static final String LOGOUT_LOCATION = "logout-location";

    private static final String NAVIGATOR_TYPE = "type";
    private static final String DESIGN_COLOR = "glass";
    private static final String WORLD_DISPLAY = "world-display";
    private static final String SLAB_BREAKING = "slab-breaking";
    private static final String NO_CLIP = "no-clip";
    private static final String TRAPDOOR = "trapdoor";
    private static final String NIGHT_VISION = "nightvision";
    private static final String SCOREBOARD = "scoreboard";
    private static final String KEEP_NAVIGATOR = "keep-navigator";
    private static final String DISABLE_INTERACT = "disable-interact";
    private static final String SPAWN_TELEPORT = "spawn-teleport";
    private static final String CLEAR_INVENTORY = "clear-inventory";
    private static final String INSTANT_PLACE_SIGNS = "instant-place-signs";
    private static final String HIDE_PLAYERS = "hide-players";
    private static final String PLACE_PLANTS = "place-plants";

    private static final String DISPLAY_SORT = "sort";
    private static final String DISPLAY_FILTER = "filter";
    private static final String FILTER_MODE = "mode";
    private static final String FILTER_TEXT = "text";

    private final Logger logger;

    public PlayerCodec(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key(BuildPlayer value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, @Nullable Object> serialize(BuildPlayer value) {
        BuildPlayerImpl player = BuildPlayerImpl.of(value);
        Map<String, @Nullable Object> serialized = new HashMap<>();

        serialized.put(SETTINGS, serializeSettings(player.getSettings()));
        LogoutLocation logoutLocation = player.getLogoutLocation();
        if (logoutLocation != null) {
            serialized.put(LOGOUT_LOCATION, LogoutLocationCodec.format(logoutLocation));
        }

        return serialized;
    }

    private Map<String, Object> serializeSettings(Settings settings) {
        Map<String, Object> serialized = new HashMap<>();

        serialized.put(NAVIGATOR_TYPE, settings.getNavigatorType().toString());
        serialized.put(DESIGN_COLOR, settings.getDesignColor().toString());
        serialized.put(WORLD_DISPLAY, serializeWorldDisplay(settings.getWorldDisplay()));
        serialized.put(SLAB_BREAKING, settings.isSlabBreaking());
        serialized.put(NO_CLIP, settings.isNoClip());
        serialized.put(TRAPDOOR, settings.isOpenTrapDoors());
        serialized.put(NIGHT_VISION, settings.isNightVision());
        serialized.put(SCOREBOARD, settings.isScoreboard());
        serialized.put(KEEP_NAVIGATOR, settings.isKeepNavigator());
        serialized.put(DISABLE_INTERACT, settings.isDisableInteract());
        serialized.put(SPAWN_TELEPORT, settings.isSpawnTeleport());
        serialized.put(CLEAR_INVENTORY, settings.isClearInventory());
        serialized.put(INSTANT_PLACE_SIGNS, settings.isInstantPlaceSigns());
        serialized.put(HIDE_PLAYERS, settings.isHidePlayers());
        serialized.put(PLACE_PLANTS, settings.isPlacePlants());

        return serialized;
    }

    private Map<String, Object> serializeWorldDisplay(WorldDisplay worldDisplay) {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put(DISPLAY_SORT, worldDisplay.getWorldSort().toString());
        serialized.put(DISPLAY_FILTER, serializeFilter(worldDisplay.getWorldFilter()));
        return serialized;
    }

    private Map<String, Object> serializeFilter(WorldFilter filter) {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put(FILTER_MODE, filter.getMode().toString());
        serialized.put(FILTER_TEXT, filter.getText());
        return serialized;
    }

    @Override
    public BuildPlayerImpl deserialize(String key, ConfigurationSection section) {
        UUID uuid = UUID.fromString(key);
        BuildPlayerImpl buildPlayer = new BuildPlayerImpl(uuid, loadSettings(section));
        buildPlayer.setLogoutLocation(LogoutLocationCodec.parse(section.getString(LOGOUT_LOCATION)));
        return buildPlayer;
    }

    private SettingsImpl loadSettings(ConfigurationSection section) {
        return SettingsImpl.builder()
                .navigatorType(parseNavigatorType(section.getString(SETTINGS + "." + NAVIGATOR_TYPE)))
                .designColor(DesignColor.matchColor(section.getString(SETTINGS + "." + DESIGN_COLOR)))
                .worldDisplay(loadWorldDisplay(section))
                .clearInventory(section.getBoolean(SETTINGS + "." + CLEAR_INVENTORY, false))
                .disableInteract(section.getBoolean(SETTINGS + "." + DISABLE_INTERACT, false))
                .hidePlayers(section.getBoolean(SETTINGS + "." + HIDE_PLAYERS, false))
                .instantPlaceSigns(section.getBoolean(SETTINGS + "." + INSTANT_PLACE_SIGNS, false))
                .keepNavigator(section.getBoolean(SETTINGS + "." + KEEP_NAVIGATOR, false))
                .nightVision(section.getBoolean(SETTINGS + "." + NIGHT_VISION, false))
                .noClip(section.getBoolean(SETTINGS + "." + NO_CLIP, false))
                .placePlants(section.getBoolean(SETTINGS + "." + PLACE_PLANTS, false))
                .scoreboard(section.getBoolean(SETTINGS + "." + SCOREBOARD, true))
                .slabBreaking(section.getBoolean(SETTINGS + "." + SLAB_BREAKING, false))
                .spawnTeleport(section.getBoolean(SETTINGS + "." + SPAWN_TELEPORT, true))
                .openTrapDoors(section.getBoolean(SETTINGS + "." + TRAPDOOR, false))
                .build();
    }

    private WorldDisplay loadWorldDisplay(ConfigurationSection section) {
        final String prefix = SETTINGS + "." + WORLD_DISPLAY + ".";
        WorldSort worldSort =
                WorldSort.matchWorldSort(section.getString(prefix + DISPLAY_SORT, WorldSort.NEWEST_FIRST.name()));
        WorldFilter.Mode filterMode = WorldFilterImpl.Mode.valueOf(
                section.getString(prefix + DISPLAY_FILTER + "." + FILTER_MODE, WorldFilter.Mode.NONE.name()));
        String filterText = section.getString(prefix + DISPLAY_FILTER + "." + FILTER_TEXT, "");
        return new WorldDisplayImpl(worldSort, new WorldFilterImpl(filterMode, filterText));
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
}
