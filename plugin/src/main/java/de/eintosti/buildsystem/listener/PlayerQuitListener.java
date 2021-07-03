package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.object.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author einTosti
 */
public class PlayerQuitListener implements Listener {
    private final BuildSystem plugin;
    private final SettingsManager settingsManager;

    public PlayerQuitListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerQuitMessage(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String joinMessage = plugin.isJoinQuitMessages() ? plugin.getString("player_quit").replace("%player%", player.getName()) : null;
        event.setQuitMessage(joinMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerMoveListener().closeNavigator(player);

        Settings settings = settingsManager.getSettings(player);
        if (settings.isNoClip()) {
            plugin.getNoClipManager().stopNoClip(player.getUniqueId());
        }

        if (settings.isScoreboard()) {
            settingsManager.stopScoreboard(player);
        }

        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }

        manageHidePlayer(player);
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player) {
        if (settingsManager.getSettings(player).isHidePlayers()) { // Show all hidden players to player
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }

        for (Player pl : Bukkit.getOnlinePlayers()) { // Show player to all players who had him/her hidden
            if (!settingsManager.getSettings(pl).isHidePlayers()) continue;
            pl.showPlayer(player);
        }
    }
}
