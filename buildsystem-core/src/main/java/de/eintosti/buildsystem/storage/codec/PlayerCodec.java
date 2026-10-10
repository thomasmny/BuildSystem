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
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.storage.codec.FieldCodec.Field;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;

/**
 * {@link Codec} for {@link BuildPlayer}s, mapping a player's settings and last logout location to and from the
 * {@code players.<uuid>} section. The settings are applied straight to the player's own {@link Settings}.
 */
@NullMarked
public final class PlayerCodec implements Codec<BuildPlayer> {

    private final Logger logger;
    private final FieldCodec<BuildPlayerImpl, BuildPlayerImpl> fields = new FieldCodec<>(
            new Field<>(
                    "settings.type",
                    player -> settings(player).getNavigatorType().toString(),
                    this::readNavigatorType,
                    (player, type) -> settings(player).setNavigatorType(type)),
            new Field<>(
                    "settings.glass",
                    player -> settings(player).getDesignColor().toString(),
                    (section, key) -> DesignColor.matchColor(section.getString(key)),
                    (player, color) -> settings(player).setDesignColor(color)),
            new Field<>(
                    "settings.world-display.sort",
                    player -> settings(player).getWorldDisplay().getWorldSort().toString(),
                    (section, key) -> WorldSort.matchWorldSort(section.getString(key, WorldSort.NEWEST_FIRST.name())),
                    (player, sort) -> settings(player).getWorldDisplay().setWorldSort(sort)),
            new Field<>(
                    "settings.world-display.filter.mode",
                    player -> filter(player).getMode().toString(),
                    (section, key) -> WorldFilter.Mode.valueOf(section.getString(key, WorldFilter.Mode.NONE.name())),
                    (player, mode) -> filter(player).setMode(mode)),
            FieldCodec.string(
                    "settings.world-display.filter.text",
                    "",
                    player -> filter(player).getText(),
                    (player, text) -> filter(player).setText(text)),
            setting("slab-breaking", false, Settings::isSlabBreaking, Settings::setSlabBreaking),
            setting("no-clip", false, Settings::isNoClip, Settings::setNoClip),
            setting("trapdoor", false, Settings::isOpenTrapDoors, Settings::setOpenTrapDoors),
            setting("nightvision", false, Settings::isNightVision, Settings::setNightVision),
            setting("scoreboard", true, Settings::isScoreboard, Settings::setScoreboard),
            setting("keep-navigator", false, Settings::isKeepNavigator, Settings::setKeepNavigator),
            setting("disable-interact", false, Settings::isDisableInteract, Settings::setDisableInteract),
            setting("spawn-teleport", true, Settings::isSpawnTeleport, Settings::setSpawnTeleport),
            setting("clear-inventory", false, Settings::isClearInventory, Settings::setClearInventory),
            setting("instant-place-signs", false, Settings::isInstantPlaceSigns, Settings::setInstantPlaceSigns),
            setting("hide-players", false, Settings::isHidePlayers, Settings::setHidePlayers),
            setting("place-plants", false, Settings::isPlacePlants, Settings::setPlacePlants),
            new Field<>(
                    "logout-location",
                    player -> {
                        LogoutLocation location = player.getLogoutLocation();
                        return location == null ? null : LogoutLocationCodec.format(location);
                    },
                    (section, key) -> LogoutLocationCodec.parse(section.getString(key)),
                    BuildPlayerImpl::setLogoutLocation));

    public PlayerCodec(Logger logger) {
        this.logger = logger;
    }

    private static Settings settings(BuildPlayerImpl player) {
        return player.getSettings();
    }

    private static WorldFilter filter(BuildPlayerImpl player) {
        return player.getSettings().getWorldDisplay().getWorldFilter();
    }

    private static Field<BuildPlayerImpl, BuildPlayerImpl, Boolean> setting(
            String key, boolean fallback, Function<Settings, Boolean> getter, BiConsumer<Settings, Boolean> setter) {
        return FieldCodec.bool(
                "settings." + key,
                fallback,
                player -> getter.apply(settings(player)),
                (player, value) -> setter.accept(settings(player), value));
    }

    @Override
    public String key(BuildPlayer value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, Object> serialize(BuildPlayer value) {
        return fields.serialize(BuildPlayerImpl.of(value));
    }

    @Override
    public BuildPlayerImpl deserialize(String key, ConfigurationSection section) {
        BuildPlayerImpl player = new BuildPlayerImpl(UUID.fromString(key), new SettingsImpl());
        fields.read(section, player);
        return player;
    }

    private NavigatorType readNavigatorType(ConfigurationSection section, String key) {
        String raw = section.getString(key);
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
