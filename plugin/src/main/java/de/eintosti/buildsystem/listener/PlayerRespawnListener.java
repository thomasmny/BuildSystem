package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.SpawnManager;
import de.eintosti.buildsystem.object.settings.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * @author einTosti
 */
public class PlayerRespawnListener implements Listener {
    private final SettingsManager settingsManager;
    private final SpawnManager spawnManager;

    public PlayerRespawnListener(BuildSystem plugin) {
        this.settingsManager = plugin.getSettingsManager();
        this.spawnManager = plugin.getSpawnManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);

        if (settings.isSpawnTeleport() && spawnManager.spawnExists()) {
            event.setRespawnLocation(spawnManager.getSpawn());
        }
    }
}
