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

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.player.CachedValues;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;

/**
 * Puts a player into an archive world's spectating state and takes them out of it again. Shared by changing worlds,
 * joining inside an archive world, quitting, and the plugin disabling and enabling again.
 */
@NullMarked
public final class ArchiveMode {

    private ArchiveMode() {}

    /**
     * Enters archive mode for a player standing in a world where building is not allowed. Joining fires no world change,
     * and neither does the plugin enabling while players are online, so both come through here.
     */
    public static void enterIfInArchiveWorld(
            Player player,
            CachedValues cachedValues,
            WorldStorage worldStorage,
            PluginConfig.Settings.Archive archive) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld != null && !buildWorld.getData().get(WorldDataKey.STATUS).isBuildingAllowed()) {
            enter(player, cachedValues, archive);
        }
    }

    /**
     * Snapshots the player's gamemode, inventory and armor, then clears them and applies the archive settings. The
     * snapshot is handed back by {@link CachedValues#resetArchiveStateIfPresent(Player)}. Does nothing for a player
     * already in archive mode, whose snapshot would otherwise be replaced by the emptied inventory.
     */
    static void enter(Player player, CachedValues cachedValues, PluginConfig.Settings.Archive archive) {
        if (cachedValues.hasArchiveState()) {
            return;
        }
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
            addArchiveInvisibility(player);
        }
    }

    /**
     * Gives or takes the archive invisibility to match the vanish setting, for a player in archive mode. Used after the
     * config is reloaded, since the setting may have changed while players were in an archive.
     */
    public static void applyVanish(Player player, CachedValues cachedValues, boolean vanish) {
        if (!cachedValues.hasArchiveState()) {
            return;
        }
        if (vanish) {
            addArchiveInvisibility(player);
        } else {
            removeArchiveInvisibility(player);
        }
    }

    /**
     * Hands back what {@link #enter} took and removes the archive invisibility. Safe to call for a player who is not in
     * archive mode. Called before the player quits as well, so the endless invisibility is never saved into their
     * player data.
     *
     * <p>Only the archive's own invisibility is removed: endless, without particles, level one. Earlier versions saved
     * it into player data, so it is also removed from players who carry it without a snapshot. Any other invisibility,
     * such as a potion, is left alone.
     */
    public static void exit(Player player, CachedValues cachedValues) {
        cachedValues.resetArchiveStateIfPresent(player);
        removeArchiveInvisibility(player);
    }

    private static void addArchiveInvisibility(Player player) {
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false),
                false);
    }

    private static void removeArchiveInvisibility(Player player) {
        PotionEffect invisibility = player.getPotionEffect(PotionEffectType.INVISIBILITY);
        if (invisibility != null && isArchiveInvisibility(invisibility)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    private static boolean isArchiveInvisibility(PotionEffect effect) {
        return effect.isInfinite() && !effect.hasParticles() && effect.getAmplifier() == 0;
    }
}
