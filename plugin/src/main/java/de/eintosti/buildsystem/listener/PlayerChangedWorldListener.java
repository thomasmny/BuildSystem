package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.ArmorStandManager;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.object.world.WorldType;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class PlayerChangedWorldListener implements Listener {
    private final BuildSystem plugin;
    private final ArmorStandManager armorStandManager;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    private final Map<UUID, GameMode> playerGamemode;
    private final Map<UUID, ItemStack[]> playerInventory;
    private final Map<UUID, ItemStack[]> playerArmor;

    public PlayerChangedWorldListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.armorStandManager = plugin.getArmorStandManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        this.playerGamemode = new HashMap<>();
        this.playerInventory = new HashMap<>();
        this.playerArmor = new HashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        World oldWorld = worldManager.getWorld(event.getFrom().getName());
        if (oldWorld != null && plugin.isUnloadWorlds()) {
            oldWorld.resetUnloadTask();
        }

        World newWorld = worldManager.getWorld(worldName);
        if (newWorld != null) {
            if (!newWorld.isPhysics()) {
                if (player.hasPermission("buildsystem.physics.message")) {
                    player.sendMessage(plugin.getString("physics_deactivated_in_world").replace("%world%", newWorld.getName()));
                }
            }
        }

        removeOldNavigator(player);
        removeBuildMode(player);
        setGoldBlock(newWorld);
        checkWorldStatus(player);

        if (settingsManager.getSettings(player).isScoreboard()) {
            plugin.forceUpdateSidebar(player);
        }
    }

    private void removeOldNavigator(Player player) {
        armorStandManager.removeArmorStands(player);
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private void removeBuildMode(Player player) {
        if (!plugin.buildPlayers.contains(player.getUniqueId())) return;
        plugin.buildPlayers.remove(player.getUniqueId());
        if (plugin.buildPlayerGamemode.containsKey(player.getUniqueId())) {
            player.setGameMode(plugin.buildPlayerGamemode.get(player.getUniqueId()));
            plugin.buildPlayerGamemode.remove(player.getUniqueId());
        }
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        player.sendMessage(plugin.getString("build_deactivated_self"));
    }

    private void setGoldBlock(World world) {
        if (world == null) return;
        if (world.getType() != WorldType.VOID) return;
        if (world.getStatus() != WorldStatus.NOT_STARTED) return;

        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld == null) return;

        if (plugin.isVoidBlock()) {
            bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
        }
    }

    @SuppressWarnings("deprecation")
    private void checkWorldStatus(Player player) {
        String worldName = player.getWorld().getName();
        World world = worldManager.getWorld(worldName);
        if (world == null) return;

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

        if (world.getStatus() == WorldStatus.ARCHIVE) {
            this.playerGamemode.put(playerUUID, player.getGameMode());
            this.playerInventory.put(playerUUID, playerInventory.getContents());
            this.playerArmor.put(playerUUID, playerInventory.getArmorContents());

            removeArmorContent(player);
            playerInventory.clear();
            playerInventory.setItem(8, inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item")));
            setSpectatorMode(player);

            if (plugin.isArchiveVanish()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        } else {
            playerInventory.setItem(8, inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item")));
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(player));
        }
    }

    private void setSpectatorMode(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
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
