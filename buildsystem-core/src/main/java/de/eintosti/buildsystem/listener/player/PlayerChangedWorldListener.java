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
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlayerChangedWorldListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final NavigatorService navigatorService;
    private final PlayerServiceImpl playerManager;
    private final SettingsService settingsManager;
    private final WorldStorageImpl worldStorage;

    public PlayerChangedWorldListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.navigatorService = plugin.getNavigatorService();
        this.playerManager = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        event.getPlayer().setAllowFlight(true);

        BuildWorld oldWorld = worldStorage.getBuildWorld(event.getFrom());
        if (oldWorld != null
                && plugin.getConfigService().current().world().unload().enabled()) {
            oldWorld.getUnloader().resetUnloadTask();
        }

        BuildWorld newWorld = worldStorage.getBuildWorld(worldName);
        if (newWorld != null
                && !newWorld.getData().isPhysics()
                && player.hasPermission("buildsystem.physics.message")) {
            plugin.getMessages()
                    .sendMessage(player, "physics_deactivated_in_world", Map.entry("%world%", newWorld.getName()));
        }

        removeOldNavigator(player);
        removeBuildMode(player);
        setGoldBlock(newWorld);
        checkWorldStatus(player);

        if (settingsManager.getSettings(player).isScoreboard()) {
            settingsManager.forceUpdateSidebar(player);
        }
    }

    private void removeOldNavigator(Player player) {
        navigatorService.removeArmorStands(player);
        player.removePotionEffect(XPotion.BLINDNESS.get());
    }

    private void removeBuildMode(Player player) {
        if (!playerManager.leaveBuildMode(player.getUniqueId())) {
            return;
        }

        CachedValues cachedValues = BuildPlayerImpl.of(
                        playerManager.getPlayerStorage().getBuildPlayer(player))
                .getCachedValues();
        cachedValues.resetGameModeIfPresent(player);
        cachedValues.resetInventoryIfPresent(player);
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        plugin.getMessages().sendMessage(player, "build_deactivated_self");
    }

    private void setGoldBlock(@Nullable BuildWorld buildWorld) {
        if (buildWorld == null
                || buildWorld.getType() != BuildWorldType.VOID
                || !buildWorld.getData().getStatus().getId().equals(WorldStatusRegistry.NOT_STARTED_ID)) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
    }

    @SuppressWarnings("deprecation")
    private void checkWorldStatus(Player player) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return;
        }

        CachedValues cachedValues = BuildPlayerImpl.of(
                        playerManager.getPlayerStorage().getBuildPlayer(player))
                .getCachedValues();
        cachedValues.resetArchiveStateIfPresent(player);

        if (!buildWorld.getData().getStatus().isBuildingAllowed()) {
            cachedValues.saveArchiveState(player);

            removeArmorContent(player);
            player.getInventory().clear();
            setSpectatorMode(player);

            if (plugin.getConfigService().current().settings().archive().vanish()) {
                player.addPotionEffect(
                        new PotionEffect(XPotion.INVISIBILITY.get(), PotionEffect.INFINITE_DURATION, 0, false, false),
                        false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        } else {
            player.removePotionEffect(XPotion.INVISIBILITY.get());
            Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(player));
        }

        navigatorService.giveNavigator(player);
    }

    private void setSpectatorMode(Player player) {
        if (plugin.getConfigService().current().settings().archive().changeGamemode()) {
            player.setGameMode(
                    plugin.getConfigService().current().settings().archive().worldGameMode());
        }
        player.setSaturation(20);
        player.setHealth(20);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    private void removeArmorContent(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.setHelmet(null);
        playerInventory.setChestplate(null);
        playerInventory.setLeggings(null);
        playerInventory.setBoots(null);
    }
}
