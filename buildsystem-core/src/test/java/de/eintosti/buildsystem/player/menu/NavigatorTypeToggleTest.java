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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.VisibilityFixture;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class NavigatorTypeToggleTest {

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void switchingToTheOldNavigatorWhileTheArmorStandOneIsOpen_closesItFully() {
        ServerMock server = MockBukkit.mock();
        VisibilityFixture fixture = new VisibilityFixture(MockBukkit.createMockPlugin());
        PlayerMock player = SoundlessPlayer.join(server, "Player");
        NavigatorService navigatorService = new NavigatorService(
                mock(NavigatorCategoryRegistryImpl.class),
                mock(ConfigService.class),
                mock(NavigatorItems.class),
                mock(MenuItems.class),
                fixture.playerService,
                mock(Messages.class),
                mock(TaskScheduler.class),
                new NamespacedKey("test", "owner"),
                new NamespacedKey("test", "category"));
        Settings settings = fixture.buildPlayer(player).getSettings();
        settings.setNavigatorType(NavigatorType.NEW);
        // What opening the navigator does to the player.
        fixture.buildPlayer(player).getCachedValues().saveSpeeds(player);
        player.setWalkSpeed(0);
        player.setFlySpeed(0);
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 250, false, false));
        navigatorService.markNavigatorOpen(player);

        new SettingToggles(fixture.settingsService, navigatorService, mock(NoClipService.class))
                .toggleNavigatorType(player, settings);

        assertEquals(NavigatorType.OLD, settings.getNavigatorType());
        assertFalse(navigatorService.isNavigatorOpen(player));
        assertEquals(0.2f, player.getWalkSpeed());
        assertEquals(0.1f, player.getFlySpeed());
        assertNull(player.getPotionEffect(PotionEffectType.JUMP_BOOST));
        assertNull(player.getPotionEffect(PotionEffectType.BLINDNESS));
    }
}
