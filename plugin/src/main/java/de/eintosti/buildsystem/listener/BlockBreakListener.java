package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * @author einTosti
 */
public class BlockBreakListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public BlockBreakListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) return;

        disableArchivedWorlds(buildWorld, player, event);
        checkWorldSettings(buildWorld, player, event);
        checkBuilders(buildWorld, player, event);
        setStatus(buildWorld, player);
    }

    private void disableArchivedWorlds(BuildWorld buildWorld, Player player, BlockBreakEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(BuildWorld buildWorld, Player player, BlockBreakEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!buildWorld.isBlockBreaking() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, BlockBreakEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return;
        }
        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
        }
    }

    private void setStatus(BuildWorld buildWorld, Player player) {
        if (buildWorld.getStatus() == WorldStatus.NOT_STARTED) {
            buildWorld.setStatus(WorldStatus.IN_PROGRESS);
            plugin.forceUpdateSidebar(player);
        }
    }
}
