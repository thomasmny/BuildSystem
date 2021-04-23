package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
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
        World world = worldManager.getWorld(worldName);
        if (world == null) return;

        disableArchivedWorlds(world, player, event);
        checkWorldSettings(world, player, event);
        checkBuilders(world, player, event);
        setStatus(world, player);
    }

    private void disableArchivedWorlds(World world, Player player, BlockBreakEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (world.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(World world, Player player, BlockBreakEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!world.isBlockBreaking() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkBuilders(World world, Player player, BlockBreakEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && world.getCreatorId() != null && world.getCreatorId().equals(player.getUniqueId())) {
            return;
        }
        if (world.isBuilders() && !world.isBuilder(player)) {
            event.setCancelled(true);
        }
    }

    private void setStatus(World world, Player player) {
        if (world.getStatus() == WorldStatus.NOT_STARTED) {
            world.setStatus(WorldStatus.IN_PROGRESS);
            plugin.forceUpdateSidebar(player);
        }
    }
}
