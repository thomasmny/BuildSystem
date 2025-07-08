/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.inventory.XInventoryView;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.CachedValues;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.config.Config.Settings.Navigator;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.world.navigator.ArmorStandManager;
import de.eintosti.buildsystem.world.navigator.inventory.ArchivedWorldsInventory;
import de.eintosti.buildsystem.world.navigator.inventory.DisplayablesInventory;
import de.eintosti.buildsystem.world.navigator.inventory.NavigatorInventory;
import de.eintosti.buildsystem.world.navigator.inventory.PrivateWorldsInventory;
import de.eintosti.buildsystem.world.navigator.inventory.PublicWorldsInventory;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NavigatorListener implements Listener {

    private static final double MAX_HEIGHT = 2.074631929397583;
    private static final double MIN_HEIGHT = 1.4409877061843872;

    private final BuildSystemPlugin plugin;
    private final ArmorStandManager armorStandManager;
    private final PlayerServiceImpl playerService;
    private final SettingsManager settingsManager;
    private final WorldStorage worldStorage;

    public NavigatorListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.armorStandManager = plugin.getArmorStandManager();
        this.playerService = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsManager();
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Called whenever the player interacts with the navigator item.
     *
     * @param event The event which calls this method
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

        if (isCloseNavigatorItem(player, itemStack)) {
            event.setCancelled(true);
            playerService.closeNavigator(player);
            return;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (xMaterial != Navigator.item || !itemMeta.getDisplayName().equals(Messages.getString("navigator_item", player))) {
            return;
        }

        if (!player.hasPermission("buildsystem.navigator.item")) {
            Messages.sendPermissionError(player);
            return;
        }

        event.setCancelled(true);
        openNavigator(player);
    }

    private void openNavigator(Player player) {
        Settings settings = settingsManager.getSettings(player);
        switch (settings.getNavigatorType()) {
            case OLD -> {
                new NavigatorInventory(plugin).openInventory(player);
                XSound.BLOCK_CHEST_OPEN.play(player);
            }
            case NEW -> {
                if (playerService.getOpenNavigator().contains(player)) {
                    Messages.sendMessage(player, "worlds_navigator_open");
                    return;
                }

                summonNewNavigator(player);
                String findItemName = Messages.getString("navigator_item", player);
                ItemStack replaceItem = InventoryUtils.createItem(XMaterial.BARRIER, Messages.getString("barrier_item", player));
                InventoryUtils.replaceItem(player, findItemName, Navigator.item, replaceItem);
            }
        }
    }

    private void summonNewNavigator(Player player) {
        CachedValues cachedValues = playerService.getPlayerStorage().getBuildPlayer(player).getCachedValues();
        cachedValues.saveWalkSpeed(player.getWalkSpeed());
        cachedValues.saveFlySpeed(player.getFlySpeed());

        // Cancel movement and disable flying
        player.setSprinting(false);
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.setVelocity(new Vector(0, 0, 0));
        player.teleport(player.getLocation());
        player.addPotionEffect(new PotionEffect(XPotion.BLINDNESS.get(), PotionEffect.INFINITE_DURATION, 0, false, false));
        player.addPotionEffect(new PotionEffect(XPotion.JUMP_BOOST.get(), PotionEffect.INFINITE_DURATION, 250, false, false));

        armorStandManager.spawnArmorStands(player);
        playerService.getOpenNavigator().add(player);
    }

    /**
     * Manages a player's interaction with the {@link NavigatorType#NEW} navigator.
     *
     * @param event The event which calls this method
     */
    @EventHandler
    public void manageNewNavigatorInteraction(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Disable interaction entities in archived worlds
        disableArchivedWorlds(player, event);

        if (!playerService.getOpenNavigator().contains(player) || !(entity instanceof ArmorStand armorStand)) {
            return;
        }

        if (isCloseNavigatorItem(player, player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            playerService.closeNavigator(player);
            return;
        }

        Vector clickedPosition = event.getClickedPosition();
        if (clickedPosition.getY() > MIN_HEIGHT && clickedPosition.getY() < MAX_HEIGHT) {
            NavigatorCategory category = ArmorStandManager.matchNavigatorCategory(armorStand);
            if (category == null) {
                return;
            }

            UUID ownerUUID = ArmorStandManager.getOwner(armorStand);
            if (!Objects.equals(ownerUUID, player.getUniqueId())) {
                return;
            }

            DisplayablesInventory inventory = switch (category) {
                case PUBLIC -> new PublicWorldsInventory(plugin, player);
                case ARCHIVE -> new ArchivedWorldsInventory(plugin, player);
                case PRIVATE -> new PrivateWorldsInventory(plugin, player);
            };

            XSound.BLOCK_CHEST_OPEN.play(player);
            inventory.openInventory();
        }
    }

    /**
     * Cancels an event if the player is in an archived world.
     *
     * @param player      The player object
     * @param cancellable The event to cancel
     */
    private void disableArchivedWorlds(Player player, Cancellable cancellable) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null || buildWorld.getData().status().get() != BuildWorldStatus.ARCHIVE) {
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
        if (ArmorStandManager.matchNavigatorCategory(armorStand) == null || ArmorStandManager.getOwner(armorStand) == null) {
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
        if (!playerService.getOpenNavigator().contains(player)) {
            return;
        }

        if (isCloseNavigatorItem(player, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the given item is the item that is used to close the {@link NavigatorType#NEW} navigator.
     *
     * @param player    The player used to get the item name
     * @param itemStack The item stack to check
     * @return {@code true} if the item is the navigator close item, {@code false} otherwise
     */
    private boolean isCloseNavigatorItem(Player player, @Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return false;
        }

        if (XMaterial.matchXMaterial(itemStack) != XMaterial.BARRIER) {
            return false;
        }

        return itemMeta.getDisplayName().equals(Messages.getString("barrier_item", player));
    }
}