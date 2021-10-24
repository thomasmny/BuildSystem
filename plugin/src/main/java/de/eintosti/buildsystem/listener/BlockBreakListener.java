package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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

        if (disableArchivedWorlds(buildWorld, player, event)) return;
        if (checkWorldSettings(buildWorld, player, event)) return;
        if (checkBuilders(buildWorld, player, event)) return;

        setStatus(buildWorld, player);
    }

    private boolean disableArchivedWorlds(BuildWorld buildWorld, Player player, BlockBreakEvent event) {
        if (!plugin.canBypass(player) && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean checkWorldSettings(BuildWorld buildWorld, Player player, BlockBreakEvent event) {
        if (!plugin.canBypass(player) && buildWorld.isBlockPlacement()) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean checkBuilders(BuildWorld buildWorld, Player player, BlockBreakEvent event) {
        if (plugin.canBypass(player)) return false;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return false;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }

    private void setStatus(BuildWorld buildWorld, Player player) {
        if (buildWorld.getStatus() == WorldStatus.NOT_STARTED) {
            buildWorld.setStatus(WorldStatus.IN_PROGRESS);
            plugin.forceUpdateSidebar(player);
        }
    }
}
