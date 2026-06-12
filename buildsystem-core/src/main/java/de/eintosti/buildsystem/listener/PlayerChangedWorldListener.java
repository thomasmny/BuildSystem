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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.CachedValues;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.navigator.ArmorStandManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlayerChangedWorldListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final ArmorStandManager armorStandManager;
    private final PlayerServiceImpl playerManager;
    private final SettingsService settingsManager;
    private final WorldStorageImpl worldStorage;

    private final Map<UUID, GameMode> playerGamemode;
    private final Map<UUID, ItemStack[]> playerInventory;
    private final Map<UUID, ItemStack[]> playerArmor;

    public PlayerChangedWorldListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.armorStandManager = plugin.getArmorStandManager();
        this.playerManager = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        this.playerGamemode = new HashMap<>();
        this.playerInventory = new HashMap<>();
        this.playerArmor = new HashMap<>();

    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        event.getPlayer().setAllowFlight(true);

        BuildWorld oldWorld = worldStorage.getBuildWorld(event.getFrom());
        if (oldWorld != null && plugin.getConfigService().current().world().unload().enabled()) {
            oldWorld.getUnloader().resetUnloadTask();
        }

        BuildWorld newWorld = worldStorage.getBuildWorld(worldName);
        if (newWorld != null && !newWorld.getData().physics().get() && player.hasPermission("buildsystem.physics.message")) {
            plugin.getMessages().sendMessage(player, "physics_deactivated_in_world", Map.entry("%world%", newWorld.getName()));
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
        armorStandManager.removeArmorStands(player);
        player.removePotionEffect(XPotion.BLINDNESS.get());
    }

    private void removeBuildMode(Player player) {
        if (!playerManager.getBuildModePlayers().remove(player.getUniqueId())) {
            return;
        }

        CachedValues cachedValues = playerManager.getPlayerStorage().getBuildPlayer(player).getCachedValues();
        cachedValues.resetGameModeIfPresent(player);
        cachedValues.resetInventoryIfPresent(player);
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        plugin.getMessages().sendMessage(player, "build_deactivated_self");
    }

    private void setGoldBlock(@Nullable BuildWorld buildWorld) {
        if (buildWorld == null || buildWorld.getType() != BuildWorldType.VOID
                || buildWorld.getData().status().get() != BuildWorldStatus.NOT_STARTED) {
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

        UUID playerUUID = player.getUniqueId();
        PlayerInventory playerInventory = player.getInventory();

        if (this.playerGamemode.containsKey(playerUUID)) {
            player.setGameMode(this.playerGamemode.get(playerUUID));
            this.playerGamemode.remove(playerUUID);
        }

        if (this.playerInventory.containsKey(playerUUID)) {
            playerInventory.clear();
            playerInventory.setContents(this.playerInventory.get(playerUUID));
            this.playerInventory.remove(playerUUID);
        }

        if (this.playerArmor.containsKey(playerUUID)) {
            removeArmorContent(player);
            playerInventory.setArmorContents(this.playerArmor.get(playerUUID));
            this.playerArmor.remove(playerUUID);
        }

        if (buildWorld.getData().status().get() == BuildWorldStatus.ARCHIVE) {
            this.playerGamemode.put(playerUUID, player.getGameMode());
            this.playerInventory.put(playerUUID, playerInventory.getContents());
            this.playerArmor.put(playerUUID, playerInventory.getArmorContents());

            removeArmorContent(player);
            playerInventory.clear();
            setSpectatorMode(player);

            if (plugin.getConfigService().current().settings().archive().vanish()) {
                player.addPotionEffect(new PotionEffect(XPotion.INVISIBILITY.get(), PotionEffect.INFINITE_DURATION, 0, false, false), false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        } else {
            player.removePotionEffect(XPotion.INVISIBILITY.get());
            Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(player));
        }

        playerManager.giveNavigator(player);
    }

    private void setSpectatorMode(Player player) {
        // Checking if the game mode should be set to adventure mode on archive worlds
        if (plugin.getConfigService().current().settings().archive().changeGamemode()) {
            player.setGameMode(plugin.getConfigService().current().settings().archive().worldGameMode());
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