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
package de.eintosti.buildsystem.listener.player;

import com.cryptomorin.xseries.XPotion;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.settings.SettingsService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;

/**
 * Puts a player into an archive world's spectating state and takes them out of it again. Shared by changing worlds,
 * joining inside an archive world and quitting.
 */
@NullMarked
final class ArchiveMode {

    private ArchiveMode() {}

    /**
     * Snapshots the player's gamemode, inventory and armor, then clears them and applies the archive settings. The
     * snapshot is handed back by {@link CachedValues#resetArchiveStateIfPresent(Player)}.
     */
    @SuppressWarnings("deprecation")
    static void enter(Player player, CachedValues cachedValues, PluginConfig.Settings.Archive archive) {
        cachedValues.saveArchiveState(player);

        PlayerInventory playerInventory = player.getInventory();
        playerInventory.setHelmet(null);
        playerInventory.setChestplate(null);
        playerInventory.setLeggings(null);
        playerInventory.setBoots(null);
        playerInventory.clear();

        if (archive.changeGamemode()) {
            player.setGameMode(archive.worldGameMode());
        }
        player.setSaturation(20);
        player.setHealth(20);
        player.setAllowFlight(true);
        player.setFlying(true);

        if (archive.vanish()) {
            player.addPotionEffect(
                    new PotionEffect(XPotion.INVISIBILITY.get(), PotionEffect.INFINITE_DURATION, 0, false, false),
                    false);
            Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
        }
    }

    /**
     * Hands back what {@link #enter} took and makes the player visible again. Safe to call for a player who is not in
     * archive mode. Called before the player quits as well, so the endless invisibility is never saved into their
     * player data.
     */
    @SuppressWarnings("deprecation")
    static void exit(Player player, CachedValues cachedValues, SettingsService settingsService) {
        cachedValues.resetArchiveStateIfPresent(player);
        player.removePotionEffect(XPotion.INVISIBILITY.get());
        Bukkit.getOnlinePlayers().stream()
                .filter(pl -> !settingsService.getSettings(pl).isHidePlayers())
                .forEach(pl -> pl.showPlayer(player));
    }
}
