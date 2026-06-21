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
package de.eintosti.buildsystem.navigator;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NavigatorService {

    private static final float RADIUS = 2.2f;
    private static final float SPREAD = 90.0f;

    private final NavigatorCategoryRegistryImpl navigatorCategoryRegistry;
    private final ConfigService configService;
    private final MenuItems menuItems;
    private final PlayerServiceImpl playerService;
    private final Messages messages;
    private final TaskScheduler scheduler;
    private final NamespacedKey ownerKey;
    private final NamespacedKey categoryKey;

    private final Set<Player> openNavigator;
    private final Map<UUID, ArmorStand[]> armorStands;

    public NavigatorService(
            NavigatorCategoryRegistryImpl navigatorCategoryRegistry,
            ConfigService configService,
            MenuItems menuItems,
            PlayerServiceImpl playerService,
            Messages messages,
            TaskScheduler scheduler,
            NamespacedKey ownerKey,
            NamespacedKey categoryKey) {
        this.navigatorCategoryRegistry = navigatorCategoryRegistry;
        this.configService = configService;
        this.menuItems = menuItems;
        this.playerService = playerService;
        this.messages = messages;
        this.scheduler = scheduler;
        this.ownerKey = ownerKey;
        this.categoryKey = categoryKey;
        this.openNavigator = new HashSet<>();
        this.armorStands = new HashMap<>();
        initEntityChecker();
    }

    public @Nullable NavigatorCategory matchNavigatorCategory(ArmorStand armorStand) {
        String categoryId = armorStand.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        return categoryId != null
                ? navigatorCategoryRegistry.getCategory(categoryId).orElse(null)
                : null;
    }

    public @Nullable UUID getOwner(ArmorStand armorStand) {
        String ownerUUID = armorStand.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return ownerUUID != null ? UUID.fromString(ownerUUID) : null;
    }

    public void spawnArmorStands(Player player) {
        List<NavigatorCategory> shownCategories = navigatorCategoryRegistry.getCategories().stream()
                .filter(NavigatorCategory::isShownInNavigator)
                .toList();

        ArmorStand[] stands = new ArmorStand[shownCategories.size()];
        for (int i = 0; i < shownCategories.size(); i++) {
            stands[i] = spawnArmorStand(player, shownCategories.get(i), spreadAngle(i, shownCategories.size()));
        }
        this.armorStands.put(player.getUniqueId(), stands);
    }

    public void removeArmorStands(Player player) {
        ArmorStand[] stands = this.armorStands.remove(player.getUniqueId());
        if (stands == null) {
            return;
        }
        for (ArmorStand armorStand : stands) {
            armorStand.remove();
        }
    }

    /**
     * Distributes the shown categories evenly across the {@link #SPREAD} arc in front of the player; a single category
     * sits dead centre.
     */
    private float spreadAngle(int index, int count) {
        if (count <= 1) {
            return 0;
        }
        return -SPREAD / 2 + (SPREAD * index / (count - 1));
    }

    private Location calculatePosition(Player player, float angle) {
        Location playerLocation = player.getLocation();
        float centerX = (float) playerLocation.getX();
        float centerZ = (float) playerLocation.getZ();
        float yaw = playerLocation.getYaw() + 180 + angle;
        float xPos = RADIUS * (float) Math.cos(Math.toRadians(yaw - 90)) + centerX;
        double yPos = playerLocation.getY() - 0.1;
        float zPos = RADIUS * (float) Math.sin(Math.toRadians(yaw - 90)) + centerZ;

        Location location = new Location(player.getWorld(), xPos, yPos, zPos);
        location.setYaw(yaw);
        return location;
    }

    private ArmorStand spawnArmorStand(Player player, NavigatorCategory category, float angle) {
        Location location = calculatePosition(player, angle);
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.getEquipment().setHelmet(helmetFor(player, category));

        PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(categoryKey, PersistentDataType.STRING, category.getId());

        return armorStand;
    }

    /**
     * Builds the floating head shown for a category, delegating the texture resolution (configured texture, viewer head
     * for added-players categories, or the navigator texture) to {@link ItemBuilder#icon(NavigatorCategory, Player)}.
     */
    private ItemStack helmetFor(Player player, NavigatorCategory category) {
        return ItemBuilder.icon(category, player).build();
    }

    public Set<Player> getOpenNavigator() {
        return Collections.unmodifiableSet(openNavigator);
    }

    public boolean isNavigatorOpen(Player player) {
        return openNavigator.contains(player);
    }

    public void markNavigatorOpen(Player player) {
        openNavigator.add(player);
    }

    public void giveNavigator(Player player) {
        if (!configService.current().settings().navigator().giveItemOnJoin()
                || !player.hasPermission("buildsystem.navigator.item")
                || menuItems.hasNavigator(player)) {
            return;
        }

        ItemStack navigatorItem = menuItems.createNavigatorItem(player);
        PlayerInventory playerInventory = player.getInventory();
        ItemStack slot8 = playerInventory.getItem(8);

        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            playerInventory.setItem(8, navigatorItem);
        } else {
            playerInventory.addItem(navigatorItem);
        }
    }

    public void closeNewNavigator(Player player) {
        if (!openNavigator.contains(player)) {
            return;
        }

        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(playerService.getPlayerStorage().getBuildPlayer(player));
        buildPlayer.setLastLookedAt(null);
        removeArmorStands(player);

        XSound.ENTITY_ITEM_BREAK.play(player);
        displayActionBarMessage(player, "");
        menuItems.replaceItem(
                player,
                messages.getString("barrier_item", player),
                XMaterial.BARRIER,
                menuItems.createNavigatorItem(player));

        CachedValues cachedValues = buildPlayer.getCachedValues();
        cachedValues.resetWalkSpeedIfPresent(player);
        cachedValues.resetFlySpeedIfPresent(player);
        player.removePotionEffect(XPotion.JUMP_BOOST.get());
        player.removePotionEffect(XPotion.BLINDNESS.get());

        openNavigator.remove(player);
    }

    private void initEntityChecker() {
        scheduler.runTimer(this::checkForArmorStandNavigator, 0L, 2L);
    }

    private void checkForArmorStandNavigator() {
        for (Player player : openNavigator) {
            ArmorStand[] stands = armorStands.get(player.getUniqueId());
            if (stands == null) {
                continue;
            }

            NavigatorCategory lookedAt = null;
            for (ArmorStand armorStand : stands) {
                if (!armorStand.isDead() && isLookingAtArmorStandHead(player, armorStand)) {
                    lookedAt = matchNavigatorCategory(armorStand);
                    break;
                }
            }
            sendTypeInfo(player, lookedAt);
        }
    }

    private boolean isLookingAtArmorStandHead(Player player, ArmorStand armorStand) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        Location standEyeLocation = armorStand.getEyeLocation().clone();
        double boxHalfSize = 0.3;

        double minX = standEyeLocation.getX() - boxHalfSize;
        double maxX = standEyeLocation.getX() + boxHalfSize;
        double minY = standEyeLocation.getY() - boxHalfSize;
        double maxY = standEyeLocation.getY() + boxHalfSize;
        double minZ = standEyeLocation.getZ() - boxHalfSize;
        double maxZ = standEyeLocation.getZ() + boxHalfSize;

        for (double distance = 0; distance <= 3; distance += 0.05) {
            Vector point = eyeLocation.toVector().add(direction.clone().multiply(distance));

            if (point.getX() >= minX
                    && point.getX() <= maxX
                    && point.getY() >= minY
                    && point.getY() <= maxY
                    && point.getZ() >= minZ
                    && point.getZ() <= maxZ) {
                return true;
            }
        }

        return false;
    }

    private void sendTypeInfo(Player player, @Nullable NavigatorCategory category) {
        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(playerService.getPlayerStorage().getBuildPlayer(player));
        if (category == null) {
            buildPlayer.setLastLookedAt(null);
            displayActionBarMessage(player, "§0");
            return;
        }

        NavigatorCategory lastLookedAt = buildPlayer.getLastLookedAt();
        if (lastLookedAt == null || !lastLookedAt.equals(category)) {
            buildPlayer.setLastLookedAt(category);
            XSound.ENTITY_CHICKEN_EGG.play(player);
        }

        displayActionBarMessage(player, ColorAPI.process(category.getStyledName()));
    }

    private void displayActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
