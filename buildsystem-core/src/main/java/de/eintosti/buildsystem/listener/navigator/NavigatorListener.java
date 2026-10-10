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
package de.eintosti.buildsystem.listener.navigator;

import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.inventory.XInventoryView;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.Permissions;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NavigatorListener implements Listener {

    private static final double MAX_HEIGHT = 2.074631929397583;
    private static final double MIN_HEIGHT = 1.4409877061843872;

    private final NavigatorService navigatorService;
    private final SettingsService settingsManager;
    private final WorldStorage worldStorage;
    private final NavigatorItems navigatorItems;
    private final Messages messages;
    private final Menus menus;
    private final PlayerService playerService;

    public NavigatorListener(
            NavigatorService navigatorService,
            SettingsService settingsManager,
            WorldStorage worldStorage,
            NavigatorItems navigatorItems,
            Messages messages,
            Menus menus,
            PlayerService playerService) {
        this.navigatorService = navigatorService;
        this.settingsManager = settingsManager;
        this.worldStorage = worldStorage;
        this.navigatorItems = navigatorItems;
        this.messages = messages;
        this.menus = menus;
        this.playerService = playerService;
    }

    /**
     * Called whenever the player interacts with the navigator item.
     *
     * @param event The event that calls this method
     */
    @EventHandler
    public void manageNavigatorItemInteraction(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Player player = event.getPlayer();
        if (XInventoryView.of(player.getOpenInventory()).getTopInventory().getType() != InventoryType.CRAFTING) {
            return;
        }

        if (navigatorItems.is(itemStack)) {
            event.setCancelled(true);
            if (!player.hasPermission(Permissions.NAVIGATOR_ITEM)) {
                messages.sendPermissionError(player);
                return;
            }
            openNavigator(player);
        } else if (navigatorItems.isBarrier(itemStack)) {
            event.setCancelled(true);
            navigatorService.closeNewNavigator(player);
        }
    }

    private void openNavigator(Player player) {
        Settings settings = settingsManager.getSettings(player);
        switch (settings.getNavigatorType()) {
            case OLD -> {
                menus.openNavigator(player);
                player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
            }
            case NEW -> {
                if (navigatorService.isNavigatorOpen(player)) {
                    messages.sendMessage(player, "worlds_navigator_open");
                    return;
                }

                summonNewNavigator(player);
                navigatorItems.replace(player, navigatorItems::is, navigatorItems.createBarrier(player));
            }
        }
    }

    private void summonNewNavigator(Player player) {
        CachedValues cachedValues = BuildPlayerImpl.of(
                        playerService.getPlayerStorage().getBuildPlayer(player))
                .getCachedValues();
        cachedValues.saveSpeeds(player);

        player.setSprinting(false);
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(player.getLocation());
        player.addPotionEffect(
                new PotionEffect(XPotion.BLINDNESS.get(), PotionEffect.INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(
                new PotionEffect(XPotion.JUMP_BOOST.get(), PotionEffect.INFINITE_DURATION, 250, false, false));

        navigatorService.spawnArmorStands(player);
        navigatorService.markNavigatorOpen(player);
    }

    /**
     * Manages a player's interaction with the {@link NavigatorType#NEW} navigator.
     *
     * @param event The event that calls this method
     */
    @EventHandler
    public void manageNewNavigatorInteraction(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        disableArchivedWorlds(player, event);

        if (!navigatorService.isNavigatorOpen(player) || !(entity instanceof ArmorStand armorStand)) {
            return;
        }

        if (navigatorItems.isBarrier(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            navigatorService.closeNewNavigator(player);
            return;
        }

        Vector clickedPosition = event.getClickedPosition();
        if (clickedPosition.getY() > MIN_HEIGHT && clickedPosition.getY() < MAX_HEIGHT) {
            NavigatorCategory category = navigatorService.matchNavigatorCategory(armorStand);
            if (category == null) {
                return;
            }

            UUID ownerUUID = navigatorService.getOwner(armorStand);
            if (!Objects.equals(ownerUUID, player.getUniqueId())) {
                return;
            }

            player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
            menus.openCategoryWorlds(category, player);
        }
    }

    /**
     * Cancels an event if the player is in an archived world.
     *
     * @param player The player object
     * @param cancellable The event to cancel
     */
    private void disableArchivedWorlds(Player player, Cancellable cancellable) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null || buildWorld.getData().get(WorldDataKey.STATUS).isBuildingAllowed()) {
            return;
        }

        if (!playerService.isInBuildMode(player)) {
            cancellable.setCancelled(true);
        }
    }

    /**
     * Disables players from manipulating with armor stands which make up the {@link NavigatorType#NEW} navigator.
     *
     * @param event The event which calls this method
     */
    @EventHandler
    public void preventNewNavigatorManipulation(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        if (navigatorService.matchNavigatorCategory(armorStand) == null
                || navigatorService.getOwner(armorStand) == null) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Prevents players from dropping the item which is used to close the {@link NavigatorType#NEW} navigator.
     *
     * @param event The event which calls this method
     */
    @EventHandler
    public void preventBarrierDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!navigatorService.isNavigatorOpen(player)) {
            return;
        }

        if (navigatorItems.isBarrier(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
