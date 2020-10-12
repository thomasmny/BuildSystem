package de.eintosti.buildsystem.manager;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.object.navigator.NavigatorType;
import de.eintosti.buildsystem.object.settings.Colour;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.settings.WorldSort;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.config.SettingsConfig;
import de.eintosti.buildsystem.version.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author einTosti
 */
public class SettingsManager {
    private final BuildSystem plugin;
    private final SettingsConfig settingsConfig;
    private final WorldManager worldManager;

    private final Map<UUID, Settings> settings;

    public SettingsManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.settingsConfig = new SettingsConfig(plugin);
        this.worldManager = plugin.getWorldManager();
        this.settings = new HashMap<>();
    }

    private void createSettings(UUID uuid) {
        if (!settings.containsKey(uuid)) {
            settings.put(uuid, new Settings());
        }
    }

    public void createSettings(Player player) {
        createSettings(player.getUniqueId());
    }

    public Settings getSettings(UUID uuid) {
        if (settings.get(uuid) == null) {
            createSettings(uuid);
        }
        return settings.get(uuid);
    }

    public Settings getSettings(Player player) {
        return getSettings(player.getUniqueId());
    }

    public void startScoreboard(Player player) {
        if (!plugin.isScoreboard()) return;

        Settings settings = getSettings(player);
        Sidebar sidebar = plugin.getSidebar();
        if (!settings.isScoreboard()) {
            stopScoreboard(player, settings, sidebar);
            return;
        }
        World world = worldManager.getWorld(player.getWorld().getName());
        sidebar.set(player, plugin.getStatus(world), plugin.getPermission(world), plugin.getProject(world),
                plugin.getCreator(world), plugin.getCreationDate(world));
        BukkitTask scoreboardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> sidebar.update(player, false,
                plugin.getStatus(world),
                plugin.getPermission(world),
                plugin.getProject(world),
                plugin.getCreator(world),
                plugin.getCreationDate(world)), 0L, 10L);
        settings.setScoreboardTask(scoreboardTask);
    }

    public void startScoreboard() {
        if (!plugin.isScoreboard()) return;
        Bukkit.getOnlinePlayers().forEach(this::startScoreboard);
    }

    private void stopScoreboard(Player player, Settings settings, Sidebar sidebar) {
        BukkitTask scoreboardTask = settings.getScoreboardTask();
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            settings.setScoreboardTask(null);
        }
        sidebar.remove(player);
    }

    public void stopScoreboard(Player player) {
        stopScoreboard(player, getSettings(player), plugin.getSidebar());
    }

    public void stopScoreboard() {
        Bukkit.getOnlinePlayers().forEach(this::stopScoreboard);
    }

    public void save() {
        settings.forEach(settingsConfig::saveSettings);
    }

    public void load() {
        FileConfiguration configuration = settingsConfig.getFile();
        ConfigurationSection configurationSection = configuration.getConfigurationSection("settings");
        if (configurationSection == null) return;

        Set<String> uuids = configurationSection.getKeys(false);
        uuids.forEach(uuid -> {
            NavigatorType navigatorType = NavigatorType.valueOf(configuration.getString("settings." + uuid + ".type"));
            Colour glassColor = configuration.getString("settings." + uuid + ".glass") != null ? Colour.valueOf(configuration.getString("settings." + uuid + ".glass")) : Colour.BLACK;
            WorldSort worldSort = WorldSort.matchWorldSort(configuration.getString("settings." + uuid + ".world-sort"));
            boolean slabBreaking = configuration.isBoolean("settings." + uuid + ".slab-breaking") && configuration.getBoolean("settings." + uuid + ".slab-breaking");
            boolean noClip = configuration.isBoolean("settings." + uuid + ".no-clip") && configuration.getBoolean("settings." + uuid + ".no-clip");
            boolean trapDoor = configuration.getBoolean("settings." + uuid + ".trapdoor");
            boolean nightVision = configuration.getBoolean("settings." + uuid + ".nightvision");
            boolean scoreboard = !configuration.isBoolean("settings." + uuid + ".scoreboard") || configuration.getBoolean("settings." + uuid + ".scoreboard");
            boolean disableInteract = configuration.isBoolean("settings." + uuid + ".disable-interact") && configuration.getBoolean("settings." + uuid + ".disable-interact");
            boolean spawnTeleport = !configuration.isBoolean("settings." + uuid + ".spawn-teleport") || configuration.getBoolean("settings." + uuid + ".spawn-teleport");
            boolean clearInventory = configuration.isBoolean("settings." + uuid + ".clear-inventory") && configuration.getBoolean("settings." + uuid + ".clear-inventory");
            boolean instantPlaceSigns = configuration.isBoolean("settings." + uuid + ".instant-place-signs") && configuration.getBoolean("settings." + uuid + ".instant-place-signs");
            boolean hidePlayers = configuration.isBoolean("settings." + uuid + ".hide-players") && configuration.getBoolean("settings." + uuid + ".hide-players");
            boolean placePlants = configuration.isBoolean("settings." + uuid + ".place-plants") && configuration.getBoolean("settings." + uuid + ".place-plants");

            this.settings.put(UUID.fromString(uuid), new Settings(navigatorType, glassColor, worldSort, slabBreaking, noClip, trapDoor, nightVision, scoreboard, disableInteract, spawnTeleport, clearInventory, instantPlaceSigns, hidePlayers, placePlants));
        });
    }
}
