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
package de.eintosti.buildsystem.player.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import org.junit.jupiter.api.Test;

class SettingsImplTest {

    @Test
    void builderDefaultsMatchTheNoArgConstructor() {
        SettingsImpl viaBuilder = SettingsImpl.builder().build();
        SettingsImpl viaNoArg = new SettingsImpl();

        assertEquals(viaNoArg.getNavigatorType(), viaBuilder.getNavigatorType());
        assertEquals(viaNoArg.getDesignColor(), viaBuilder.getDesignColor());
        assertEquals(viaNoArg.isScoreboard(), viaBuilder.isScoreboard());
        assertEquals(viaNoArg.isSpawnTeleport(), viaBuilder.isSpawnTeleport());
        assertEquals(viaNoArg.isClearInventory(), viaBuilder.isClearInventory());
        assertEquals(viaNoArg.isNightVision(), viaBuilder.isNightVision());
    }

    @Test
    void defaultsApplyWhenOptionsAreLeftUnset() {
        SettingsImpl settings = SettingsImpl.builder().build();

        assertEquals(NavigatorType.OLD, settings.getNavigatorType());
        assertEquals(DesignColor.BLACK, settings.getDesignColor());
        // The two opt-out toggles default on; everything else defaults off.
        assertTrue(settings.isScoreboard());
        assertTrue(settings.isSpawnTeleport());
        assertFalse(settings.isNightVision());
        assertFalse(settings.isNoClip());
        assertFalse(settings.isClearInventory());
    }

    @Test
    void nullEnumsKeepTheirDefaults() {
        SettingsImpl settings =
                SettingsImpl.builder().navigatorType(null).designColor(null).build();

        assertEquals(NavigatorType.OLD, settings.getNavigatorType());
        assertEquals(DesignColor.BLACK, settings.getDesignColor());
    }

    @Test
    void settersBindByNameAndOverrideDefaults() {
        SettingsImpl settings = SettingsImpl.builder()
                .navigatorType(NavigatorType.NEW)
                .designColor(DesignColor.RED)
                .scoreboard(false)
                .nightVision(true)
                .noClip(true)
                .build();

        assertEquals(NavigatorType.NEW, settings.getNavigatorType());
        assertEquals(DesignColor.RED, settings.getDesignColor());
        assertFalse(settings.isScoreboard());
        assertTrue(settings.isNightVision());
        assertTrue(settings.isNoClip());
        // Untouched options retain their defaults.
        assertTrue(settings.isSpawnTeleport());
        assertFalse(settings.isHidePlayers());
    }
}
