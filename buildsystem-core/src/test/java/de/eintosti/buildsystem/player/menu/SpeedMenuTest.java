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
package de.eintosti.buildsystem.player.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.menu.SpeedMenu.SpeedOption;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Golden test pinning the {@link SpeedMenu} slot &rarr; speed selection grid. Built through the package-private,
 * Bukkit-free constructor so no server is required.
 *
 * <p>Out of scope: executing a click that applies a speed (calls into Bukkit). The mapping below is the regression net.
 */
@NullMarked
class SpeedMenuTest {

    private static SpeedMenu menu() {
        SettingsService settingsService = mock(SettingsService.class);
        Messages messages = mock(Messages.class);
        Inventory inventory = mock(Inventory.class);
        return new SpeedMenu(settingsService, messages, inventory);
    }

    @Test
    void speedBySlot_hasFiveEntriesMappedToTheirSpeeds() {
        Map<Integer, SpeedOption> speeds = menu().speedBySlot();

        assertEquals(5, speeds.size());
        assertSpeed(speeds.get(11), 0.2f, 1);
        assertSpeed(speeds.get(12), 0.4f, 2);
        assertSpeed(speeds.get(13), 0.6f, 3);
        assertSpeed(speeds.get(14), 0.8f, 4);
        assertSpeed(speeds.get(15), 1.0f, 5);
    }

    @Test
    void speedBySlot_fillerSlotIsAbsent() {
        assertFalse(menu().speedBySlot().containsKey(0));
    }

    private static void assertSpeed(SpeedOption option, float expectedSpeed, int expectedDisplayNumber) {
        assertEquals(expectedSpeed, option.speed());
        assertEquals(expectedDisplayNumber, option.displayNumber());
    }
}
