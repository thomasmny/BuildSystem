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
package de.eintosti.buildsystem.player;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.CachedValues;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.storage.PlayerStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.config.Config.Settings;
import de.eintosti.buildsystem.config.Config.Settings.Navigator;
import de.eintosti.buildsystem.config.Config.World.Limits;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.factory.PlayerStorageFactory;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.world.navigator.ArmorStandManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlayerServiceImpl implements PlayerService {

    private static final double MIN_LOOK_HEIGHT = -0.16453003708696978;
    private static final double MAX_LOOK_HEIGHT = 0.16481381407766063;

    private final BuildSystemPlugin plugin;
    private final PlayerStorageImpl playerStorage;

    private final Set<Player> openNavigator;
    private final Set<UUID> buildModePlayers;

    public PlayerServiceImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerStorage = new PlayerStorageFactory(plugin).createStorage();

        this.openNavigator = new HashSet<>();
        this.buildModePlayers = new HashSet<>();

        initEntityChecker();
    }

    public void init() {
        this.playerStorage.loadPlayers();
    }

    @Override
    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public Set<Player> getOpenNavigator() {
        return openNavigator;
    }

    @Override
    public Set<UUID> getBuildModePlayers() {
        return buildModePlayers;
    }

    @Override
    public boolean isInBuildMode(Player player) {
        return buildModePlayers.contains(player.getUniqueId());
    }

    @Override
    public boolean canCreateWorld(Player player, Visibility visibility) {
        boolean showPrivateWorlds = visibility == Visibility.PRIVATE;
        WorldStorageImpl worldStorage = plugin.getWorldService().getWorldStorage();

        int maxWorldAmountConfig = showPrivateWorlds
                ? Limits.privateWorlds
                : Limits.publicWorlds;
        if (maxWorldAmountConfig >= 0 && worldStorage.getBuildWorlds().size() >= maxWorldAmountConfig) {
            return false;
        }

        int maxWorldAmountPlayer = getMaxWorlds(player, showPrivateWorlds ? Visibility.PRIVATE : Visibility.PUBLIC);
        return maxWorldAmountPlayer < 0 || worldStorage.getBuildWorldsCreatedByPlayer(player, visibility).size() < maxWorldAmountPlayer;
    }

    @Override
    public int getMaxWorlds(Player player, Visibility visibility) {
        int max = -1;
        if (player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            return max;
        }

        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String permissionString = permission.getPermission();
            String[] splitPermission = permissionString.split("\\.");

            if (splitPermission.length != 4) {
                continue;
            }

            if (!splitPermission[1].equalsIgnoreCase("create")) {
                continue;
            }

            if (!splitPermission[2].equalsIgnoreCase(visibility.name())) {
                continue;
            }

            String amountString = splitPermission[3];
            if (amountString.equals("*")) {
                return -1;
            }

            try {
                int amount = Integer.parseInt(amountString);
                if (amount > max) {
                    max = amount;
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().log(Level.WARNING, "Invalid max. world amount (must be int)", e);
            }
        }

        return max;
    }

    public void forceUpdateSidebar(BuildWorld buildWorld) {
        if (!Settings.scoreboard) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        bukkitWorld.getPlayers().forEach(this::forceUpdateSidebar);
    }

    public void forceUpdateSidebar(Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        if (!Settings.scoreboard || !settingsManager.getSettings(player).isScoreboard()) {
            return;
        }
        settingsManager.updateScoreboard(player);
    }

    public void giveNavigator(Player player) {
        if (!Navigator.giveItemOnJoin) {
            return;
        }

        if (!player.hasPermission("buildsystem.navigator.item")) {
            return;
        }

        if (InventoryUtils.hasNavigator(player)) {
            return;
        }

        ItemStack itemStack = InventoryUtils.createItem(
                Navigator.item,
                Messages.getString("navigator_item", player)
        );
        PlayerInventory playerInventory = player.getInventory();
        ItemStack slot8 = playerInventory.getItem(8);

        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            playerInventory.setItem(8, itemStack);
        } else {
            playerInventory.addItem(itemStack);
        }
    }

    public void closeNavigator(Player player) {
        if (!openNavigator.contains(player)) {
            return;
        }

        BuildPlayer buildPlayer = playerStorage.getBuildPlayer(player);
        buildPlayer.setLastLookedAt(null);
        plugin.getArmorStandManager().removeArmorStands(player);

        XSound.ENTITY_ITEM_BREAK.play(player);
        displayActionBarMessage(player, "");
        replaceBarrier(player);

        CachedValues cachedValues = buildPlayer.getCachedValues();
        cachedValues.resetWalkSpeedIfPresent(player);
        cachedValues.resetFlySpeedIfPresent(player);
        player.removePotionEffect(XPotion.JUMP_BOOST.get());
        player.removePotionEffect(XPotion.BLINDNESS.get());

        openNavigator.remove(player);
    }

    private void replaceBarrier(Player player) {
        InventoryUtils.replaceItem(
                player,
                Messages.getString("barrier_item", player),
                XMaterial.BARRIER,
                InventoryUtils.createItem(
                        Navigator.item,
                        Messages.getString("navigator_item", player)
                )
        );
    }

    private void initEntityChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForAmorStandNavigator, 0L, 2L);
    }

    /**
     * Checks if players with open navigators are looking at armor stands that represent a {@link NavigatorCategory} and updates their UI accordingly.
     * <p>
     * This method iterates through all players who have the navigator open and determines if they are looking at an armor stand within the valid look height range. When a player
     * looks at a different navigator category or stops looking at one entirely, appropriate UI updates are sent.
     */
    private void checkForAmorStandNavigator() {
        for (Player player : openNavigator) {
            if (!(getTargetEntity(player) instanceof ArmorStand armorStand)) {
                continue;
            }

            BuildPlayer buildPlayer = playerStorage.getBuildPlayer(player);
            if (isLookingAtArmorStandHead(player, armorStand)) {
                NavigatorCategory category = ArmorStandManager.matchNavigatorCategory(armorStand);
                sendTypeInfo(player, category);
            } else {
                sendTypeInfo(player, null);
            }
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

            if (point.getX() >= minX && point.getX() <= maxX
                    && point.getY() >= minY && point.getY() <= maxY
                    && point.getZ() >= minZ && point.getZ() <= maxZ) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private Entity getTargetEntity(Entity entity) {
        return getTarget(entity, entity.getNearbyEntities(3, 3, 3));
    }

    @Nullable
    @Contract("null, _ -> null")
    private <T extends Entity> T getTarget(@Nullable Entity entity, Iterable<T> entities) {
        if (entity == null) {
            return null;
        }

        T target = null;
        final Location entityLocation = entity.getLocation();
        final double threshold = 0.5;

        for (T other : entities) {
            final Location otherLocation = other.getLocation();
            final Vector vector = otherLocation.toVector().subtract(entityLocation.toVector());

            if (entityLocation.getDirection().normalize().crossProduct(vector).lengthSquared() < threshold
                    && vector.normalize().dot(entityLocation.getDirection().normalize()) >= 0) {
                if (target == null
                        || target.getLocation().distanceSquared(entityLocation) > otherLocation.distanceSquared(entityLocation)) {
                    target = other;
                }
            }
        }

        return target;
    }

    private void sendTypeInfo(Player player, @Nullable NavigatorCategory category) {
        BuildPlayer buildPlayer = playerStorage.getBuildPlayer(player);
        if (category == null) {
            buildPlayer.setLastLookedAt(null);
            displayActionBarMessage(player, "§0");
            return;
        }

        NavigatorCategory lastLookedAt = buildPlayer.getLastLookedAt();
        if (lastLookedAt == null || lastLookedAt != category) {
            buildPlayer.setLastLookedAt(category);
            XSound.ENTITY_CHICKEN_EGG.play(player);
        }

        String message = switch (category) {
            case PUBLIC -> "new_navigator_world_navigator";
            case ARCHIVE -> "new_navigator_world_archive";
            case PRIVATE -> "new_navigator_private_worlds";
        };
        displayActionBarMessage(player, Messages.getString(message, player));
    }

    private void displayActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void save() {
        this.playerStorage
                .save(this.playerStorage.getBuildPlayers())
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
                    throw new CompletionException("Failed to save player data", e);
                });
    }
}