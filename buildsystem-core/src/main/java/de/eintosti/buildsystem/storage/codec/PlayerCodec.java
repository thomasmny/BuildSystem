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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Codec} for {@link BuildPlayer}s, mapping a player's settings and last logout location to and from the
 * {@code players.<uuid>} section.
 */
@NullMarked
public final class PlayerCodec implements Codec<BuildPlayer> {

    /**
     * The on/off settings, each with its key under {@code settings} and the value a new player has.
     */
    private static final List<Toggle> TOGGLES = List.of(
            new Toggle("slab-breaking", false, Settings::isSlabBreaking, Settings::setSlabBreaking),
            new Toggle("no-clip", false, Settings::isNoClip, Settings::setNoClip),
            new Toggle("trapdoor", false, Settings::isOpenTrapDoors, Settings::setOpenTrapDoors),
            new Toggle("nightvision", false, Settings::isNightVision, Settings::setNightVision),
            new Toggle("scoreboard", true, Settings::isScoreboard, Settings::setScoreboard),
            new Toggle("keep-navigator", false, Settings::isKeepNavigator, Settings::setKeepNavigator),
            new Toggle("disable-interact", false, Settings::isDisableInteract, Settings::setDisableInteract),
            new Toggle("spawn-teleport", true, Settings::isSpawnTeleport, Settings::setSpawnTeleport),
            new Toggle("clear-inventory", false, Settings::isClearInventory, Settings::setClearInventory),
            new Toggle("instant-place-signs", false, Settings::isInstantPlaceSigns, Settings::setInstantPlaceSigns),
            new Toggle("hide-players", false, Settings::isHidePlayers, Settings::setHidePlayers),
            new Toggle("place-plants", false, Settings::isPlacePlants, Settings::setPlacePlants));

    private record Toggle(
            String key, boolean fallback, Predicate<Settings> getter, BiConsumer<Settings, Boolean> setter) {}

    private final Logger logger;

    public PlayerCodec(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key(BuildPlayer value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, Object> serialize(BuildPlayer value) {
        BuildPlayerImpl player = BuildPlayerImpl.of(value);
        Settings settings = player.getSettings();
        WorldDisplay display = settings.getWorldDisplay();

        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("mode", display.getWorldFilter().getMode().toString());
        filter.put("text", display.getWorldFilter().getText());
        Map<String, Object> worldDisplay = new LinkedHashMap<>();
        worldDisplay.put("sort", display.getWorldSort().toString());
        worldDisplay.put("filter", filter);

        Map<String, Object> serializedSettings = new LinkedHashMap<>();
        serializedSettings.put("type", settings.getNavigatorType().toString());
        serializedSettings.put("glass", settings.getDesignColor().toString());
        serializedSettings.put("world-display", worldDisplay);
        for (Toggle toggle : TOGGLES) {
            serializedSettings.put(toggle.key(), toggle.getter().test(settings));
        }

        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("settings", serializedSettings);
        LogoutLocation logoutLocation = player.getLogoutLocation();
        if (logoutLocation != null) {
            serialized.put("logout-location", LogoutLocationCodec.format(logoutLocation));
        }
        return serialized;
    }

    @Override
    public BuildPlayerImpl deserialize(String key, ConfigurationSection section) {
        SettingsImpl settings = new SettingsImpl();
        settings.setNavigatorType(parseNavigatorType(section.getString("settings.type")));
        settings.setDesignColor(DesignColor.matchColor(section.getString("settings.glass", DesignColor.BLACK.name())));
        for (Toggle toggle : TOGGLES) {
            toggle.setter().accept(settings, section.getBoolean("settings." + toggle.key(), toggle.fallback()));
        }

        WorldDisplay display = settings.getWorldDisplay();
        display.setWorldSort(WorldSort.matchWorldSort(
                section.getString("settings.world-display.sort", WorldSort.NEWEST_FIRST.name())));
        display.getWorldFilter()
                .setMode(WorldFilter.Mode.valueOf(
                        section.getString("settings.world-display.filter.mode", WorldFilter.Mode.NONE.name())));
        display.getWorldFilter().setText(section.getString("settings.world-display.filter.text", ""));

        BuildPlayerImpl player = new BuildPlayerImpl(UUID.fromString(key), settings);
        player.setLogoutLocation(LogoutLocationCodec.parse(section.getString("logout-location")));
        return player;
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
