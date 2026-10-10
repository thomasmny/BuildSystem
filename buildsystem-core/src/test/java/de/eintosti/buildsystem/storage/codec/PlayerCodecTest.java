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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Pins the stored player format: a file written by 4.0 loads with the same values and writes back the same keys, and
 * keys missing from older files fall back to the same defaults.
 */
@NullMarked
class PlayerCodecTest {

    private final PlayerCodec codec = new PlayerCodec(Logger.getLogger("PlayerCodecTest"));

    @Test
    void writtenPlayers_keepTheirKeysInDeclarationOrder() {
        BuildPlayerImpl full = CodecSamples.fullPlayer();
        BuildPlayerImpl minimal = CodecSamples.minimalPlayer();
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        entries.put(codec.key(full), codec.serialize(full));
        entries.put(codec.key(minimal), codec.serialize(minimal));

        assertEquals(CodecSamples.resource("players-written.yml"), CodecSamples.toYaml(entries));
    }

    @Test
    void fileWrittenBy40_writesBackTheSameValues() {
        String yaml = CodecSamples.resource("players-4.0.yml");
        for (String key : List.of(CodecSamples.PLAYER_ID.toString(), CodecSamples.MINIMAL_PLAYER_ID.toString())) {
            ConfigurationSection section = CodecSamples.section(yaml, key);

            BuildPlayerImpl player = codec.deserialize(key, section);

            assertEquals(CodecSamples.asMap(section), CodecSamples.reparsed(codec.serialize(player)), key);
        }
    }

    @Test
    void fileWrittenBy40_loadsEveryField() {
        String key = CodecSamples.PLAYER_ID.toString();

        String yaml = CodecSamples.resource("players-4.0.yml");
        BuildPlayerImpl player = codec.deserialize(key, CodecSamples.section(yaml, key));
        // Settings are read into each player's own objects, so a later player cannot change this one's.
        String minimalKey = CodecSamples.MINIMAL_PLAYER_ID.toString();
        codec.deserialize(minimalKey, CodecSamples.section(yaml, minimalKey));

        Settings settings = player.getSettings();
        assertEquals(CodecSamples.PLAYER_ID, player.getUniqueId());
        assertEquals(NavigatorType.NEW, settings.getNavigatorType());
        assertEquals(DesignColor.RED, settings.getDesignColor());
        assertEquals(WorldSort.PROJECT_Z_TO_A, settings.getWorldDisplay().getWorldSort());
        assertEquals(
                WorldFilter.Mode.STARTS_WITH,
                settings.getWorldDisplay().getWorldFilter().getMode());
        assertEquals("lob", settings.getWorldDisplay().getWorldFilter().getText());
        assertTrue(settings.isClearInventory());
        assertTrue(settings.isDisableInteract());
        assertTrue(settings.isHidePlayers());
        assertTrue(settings.isInstantPlaceSigns());
        assertTrue(settings.isKeepNavigator());
        assertTrue(settings.isNightVision());
        assertTrue(settings.isNoClip());
        assertTrue(settings.isPlacePlants());
        assertFalse(settings.isScoreboard());
        assertTrue(settings.isSlabBreaking());
        assertFalse(settings.isSpawnTeleport());
        assertTrue(settings.isOpenTrapDoors());
        LogoutLocation logout = Objects.requireNonNull(player.getLogoutLocation());
        assertEquals("maps:lobby:1.5:64.0:-3.25:90.0:-10.0", logout.toString());
    }

    @Test
    void sparsePlayer_fallsBackToDefaults() {
        BuildPlayerImpl player = load("""
                settings:
                  glass: RED
                """);

        Settings settings = player.getSettings();
        assertEquals(NavigatorType.OLD, settings.getNavigatorType());
        assertEquals(WorldSort.NEWEST_FIRST, settings.getWorldDisplay().getWorldSort());
        assertEquals(
                WorldFilter.Mode.NONE,
                settings.getWorldDisplay().getWorldFilter().getMode());
        assertEquals("", settings.getWorldDisplay().getWorldFilter().getText());
        assertFalse(settings.isClearInventory());
        assertFalse(settings.isNoClip());
        assertTrue(settings.isScoreboard());
        assertTrue(settings.isSpawnTeleport());
        assertNull(player.getLogoutLocation());
    }

    @Test
    void unknownValues_fallBackToDefaults() {
        BuildPlayerImpl player = load("""
                settings:
                  type: SIDEWAYS
                  glass: PLAID
                  world-display:
                    sort: BY_MOOD
                """);

        Settings settings = player.getSettings();
        assertEquals(NavigatorType.OLD, settings.getNavigatorType());
        assertEquals(DesignColor.BLACK, settings.getDesignColor());
        assertEquals(WorldSort.NAME_A_TO_Z, settings.getWorldDisplay().getWorldSort());
    }

    private BuildPlayerImpl load(String body) {
        String key = CodecSamples.PLAYER_ID.toString();
        String yaml = key + ":\n" + body.indent(2);
        return codec.deserialize(key, CodecSamples.section(yaml, key));
    }
}
