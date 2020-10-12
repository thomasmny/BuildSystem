package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author einTosti
 */
public class PlayerTeleportListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    private final HashMap<UUID, Location> previousLocation;

    public PlayerTeleportListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        this.previousLocation = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            previousLocation.put(player.getUniqueId(), event.getFrom());
        }

        Location to = event.getTo();
        if (to == null) return;
        org.bukkit.World toWorld = to.getWorld();
        if (toWorld == null) return;

        String worldName = to.getWorld().getName();
        World world = worldManager.getWorld(worldName);

        if (world == null) return;
        if (!Bukkit.getWorlds().get(0).equals(Bukkit.getWorld(worldName))) {
            if (!player.hasPermission(world.getPermission()) && !world.getPermission().equalsIgnoreCase("-")) {
                player.sendMessage(plugin.getString("worlds_tp_entry_forbidden"));
                event.setCancelled(true);
            }
        }
    }

    public Location getPreviousLocation(Player player) {
        return previousLocation.get(player.getUniqueId());
    }

    public void resetPreviousLocation(Player player) {
        previousLocation.remove(player.getUniqueId());
    }
}
