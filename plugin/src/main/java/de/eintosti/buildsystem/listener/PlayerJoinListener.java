package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.SpawnManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.util.external.UpdateChecker;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author einTosti
 */
public class PlayerJoinListener implements Listener {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;
    private final SpawnManager spawnManager;
    private final WorldManager worldManager;

    public PlayerJoinListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
        this.spawnManager = plugin.getSpawnManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerJoinMessage(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinMessage = plugin.isJoinQuitMessages() ? plugin.getString("player_join").replace("%player%", player.getName()) : null;
        event.setJoinMessage(joinMessage);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        settingsManager.createSettings(player);
        plugin.getSkullCache().cacheSkull(player.getName());

        String worldName = player.getWorld().getName();
        World world = worldManager.getWorld(worldName);
        if (world != null) {
            if (!world.isPhysics()) {
                if (player.hasPermission("buildsystem.physics.message")) {
                    player.sendMessage(plugin.getString("physics_deactivated_in_world").replace("%world%", world.getName()));
                }
            }

            if (plugin.isArchiveVanish()) {
                if (world.getStatus() == WorldStatus.ARCHIVE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), false);
                    Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
                }
            }
        }

        Settings settings = settingsManager.getSettings(player);
        if (settings.isNoClip()) {
            plugin.getNoClipManager().startNoClip(player);
        }
        if (settings.isScoreboard()) {
            settingsManager.startScoreboard(player);
            plugin.forceUpdateSidebar(player);
        }
        if (settings.isSpawnTeleport()) {
            spawnManager.teleport(player);
        }
        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }

        manageHidePlayer(player);
        addJoinItem(player);

        if (plugin.isUpdateChecker()) {
            if (player.hasPermission("buildsystem.updates")) {
                performUpdateCheck(player);
            }
        }
    }

    private void addJoinItem(Player player) {
        if (!player.hasPermission("buildsystem.gui")) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        if (playerInventory.contains(inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item")))) {
            return;
        }

        ItemStack itemStack = inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item"));
        ItemStack slot8 = playerInventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.parseMaterial()) {
            playerInventory.setItem(8, itemStack);
        } else {
            playerInventory.addItem(itemStack);
        }
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player) {
        if (settingsManager.getSettings(player).isHidePlayers()) { // Hide all players to player
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        }

        for (Player pl : Bukkit.getOnlinePlayers()) { // Hide player to all players who have hidePlayers enabled
            if (!settingsManager.getSettings(pl).isHidePlayers()) continue;
            pl.hidePlayer(player);
        }
    }

    private void performUpdateCheck(Player player) {
        UpdateChecker.init(plugin, BuildSystem.PLUGIN_ID)
                .requestUpdateCheck()
                .whenComplete((result, e) -> {
                    if (result.requiresUpdate()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        plugin.getStringList("update_available").forEach(line ->
                                stringBuilder.append(line
                                        .replace("%new_version%", result.getNewestVersion())
                                        .replace("%current_version%", plugin.getDescription().getVersion()))
                                        .append("\n"));
                        player.sendMessage(stringBuilder.toString());
                    }
                });
    }
}
