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

import com.cryptomorin.xseries.XPotion;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;

/**
 * The side-effecting halves of {@link SettingsMenu}'s toggles: flipping a player setting that also has to touch the
 * world — potion effects, no-clip, the scoreboard, player visibility, the navigator armor stands. Kept apart from the
 * menu so the behavior can be unit-tested without opening an inventory, leaving the menu to hold only wiring.
 */
@NullMarked
final class SettingToggles {

    private final SettingsService settingsManager;
    private final NavigatorService navigatorService;
    private final NoClipService noClipService;

    SettingToggles(SettingsService settingsManager, NavigatorService navigatorService, NoClipService noClipService) {
        this.settingsManager = settingsManager;
        this.navigatorService = navigatorService;
        this.noClipService = noClipService;
    }

    void toggleNavigatorType(Player player, Settings settings) {
        if (settings.getNavigatorType() == NavigatorType.OLD) {
            settings.setNavigatorType(NavigatorType.NEW);
        } else {
            settings.setNavigatorType(NavigatorType.OLD);
            navigatorService.removeArmorStands(player);
            player.removePotionEffect(XPotion.BLINDNESS.get());
        }
    }

    void toggleNightVision(Player player, Settings settings) {
        if (settings.isNightVision()) {
            settings.setNightVision(false);
            player.removePotionEffect(XPotion.NIGHT_VISION.get());
        } else {
            settings.setNightVision(true);
            player.addPotionEffect(
                    new PotionEffect(XPotion.NIGHT_VISION.get(), PotionEffect.INFINITE_DURATION, 0, false, false));
        }
    }

    void toggleNoClip(Player player, Settings settings) {
        if (settings.isNoClip()) {
            settings.setNoClip(false);
            noClipService.stopNoClip(player.getUniqueId());
        } else {
            settings.setNoClip(true);
            noClipService.startNoClip(player);
        }
    }

    /**
     * Flips the scoreboard setting, refusing (and reporting {@code false}) when the scoreboard is disabled in config so
     * the menu can play the reject sound without re-opening.
     */
    boolean toggleScoreboard(Player player, Settings settings, boolean scoreboardEnabled) {
        if (!scoreboardEnabled) {
            return false;
        }

        if (settings.isScoreboard()) {
            settings.setScoreboard(false);
            settingsManager.hideScoreboard(player);
        } else {
            settings.setScoreboard(true);
            settingsManager.displayScoreboard(player);
            settingsManager.forceUpdateSidebar(player);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    void toggleHidePlayers(Player player, Settings settings) {
        if (settings.isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        } else {
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }
    }
}
