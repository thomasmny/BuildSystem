package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.ArmorStandManager;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.object.navigator.NavigatorType;
import de.eintosti.buildsystem.object.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * @author einTosti
 */
public class PlayerMoveListener implements Listener {
    private final static double MIN_HEIGHT = -0.16453003708696978;
    private final static double MAX_HEIGHT = 0.16481381407766063;

    private final BuildSystem plugin;
    private final ArmorStandManager armorStandManager;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;

    private final Map<UUID, String> lastLookedAt;

    public PlayerMoveListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.armorStandManager = plugin.getArmorStandManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();

        this.lastLookedAt = new HashMap<>();
        initEntityChecker();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.openNavigator.contains(player)) return;
        Settings settings = settingsManager.getSettings(player);
        if (!settings.getNavigatorType().equals(NavigatorType.NEW)) return;

        Location to = event.getTo();
        if (to == null) return;
        Location from = event.getFrom();
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> closeNavigator(player), 5L);
        }
    }

    public void closeNavigator(Player player) {
        if (!plugin.openNavigator.contains(player)) return;
        lastLookedAt.remove(player.getUniqueId());
        armorStandManager.removeArmorStands(player);

        XSound.ENTITY_ITEM_BREAK.play(player);
        ActionBar.clearActionBar(player);
        replaceBarrier(player);

        UUID playerUuid = player.getUniqueId();
        player.setWalkSpeed(plugin.playerWalkSpeed.getOrDefault(playerUuid, 0.2f));
        player.setFlySpeed(plugin.playerFlySpeed.getOrDefault(playerUuid, 0.2f));
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        plugin.playerWalkSpeed.remove(playerUuid);
        plugin.playerFlySpeed.remove(playerUuid);
        plugin.openNavigator.remove(player);
    }

    private void replaceBarrier(Player player) {
        if (!player.hasPermission("buildsystem.gui")) return;

        String findItemName = plugin.getString("barrier_item");
        ItemStack replaceItem = inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item"));

        inventoryManager.replaceItem(player, findItemName, XMaterial.BARRIER, replaceItem);
    }

    private void initEntityChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForEntity, 0L, 1L);
    }

    private void checkForEntity() {
        List<UUID> toRemove = new ArrayList<>();

        for (Player player : plugin.openNavigator) {
            if (getEntityName(player) == null) continue;

            double lookedPosition = player.getEyeLocation().getDirection().getY();
            if (lookedPosition >= MIN_HEIGHT && lookedPosition <= MAX_HEIGHT) {
                String invType = getEntityName(player).replace(player.getName() + " × ", "");
                if (!lastLookedAt.containsKey(player.getUniqueId())) {
                    lastLookedAt.put(player.getUniqueId(), invType);
                    sendTypeInfo(player, invType);
                } else {
                    if (!invType.equals(lastLookedAt.get(player.getUniqueId()))) {
                        lastLookedAt.put(player.getUniqueId(), invType);
                        sendTypeInfo(player, invType);
                    }
                }
            } else {
                ActionBar.sendActionBar(player, "");
                toRemove.add(player.getUniqueId());
            }
        }

        toRemove.forEach(lastLookedAt::remove);
    }

    private <T extends Entity> T getTarget(final Entity entity, final Iterable<T> entities) {
        if (entity == null) return null;
        T target = null;

        final double threshold = 0.5;
        for (final T other : entities) {
            final Vector vector = other.getLocation().toVector().subtract(entity.getLocation().toVector());
            if (entity.getLocation().getDirection().normalize().crossProduct(vector).lengthSquared() < threshold && vector.normalize().dot(entity.getLocation().getDirection().normalize()) >= 0) {
                if (target == null || target.getLocation().distanceSquared(entity.getLocation()) > other.getLocation().distanceSquared(entity.getLocation())) {
                    target = other;
                }
            }
        }
        return target;
    }

    private Entity getTargetEntity(final Entity entity) {
        return getTarget(entity, entity.getNearbyEntities(3, 3, 3));
    }

    private String getEntityName(Player player) {
        if (getTargetEntity(player) == null) return "";
        if (getTargetEntity(player).getType() != EntityType.ARMOR_STAND) return "";

        Entity entity = getTargetEntity(player);
        if (entity.getCustomName() == null) return "";

        return entity.getCustomName();
    }

    private void sendTypeInfo(Player player, String invType) {
        String message;
        switch (invType) {
            default:
                ActionBar.sendActionBar(player, "");
                return;
            case "§aWorld Navigator":
                message = "new_navigator_world_navigator";
                break;
            case "§6World Archive":
                message = "new_navigator_world_archive";
                break;
            case "§bPrivate Worlds":
                message = "new_navigator_private_worlds";
                break;
        }

        ActionBar.sendActionBar(player, plugin.getString(message));
        XSound.ENTITY_CHICKEN_EGG.play(player);
    }
}
