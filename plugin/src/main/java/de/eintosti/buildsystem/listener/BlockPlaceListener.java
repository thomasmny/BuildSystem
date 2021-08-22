package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public class BlockPlaceListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public BlockPlaceListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);

        if (buildWorld == null) return;
        disableArchivedWorlds(buildWorld, player, event);
        checkWorldSettings(buildWorld, player, event);
        checkBuilders(buildWorld, player, event);
        setStatus(buildWorld, player);

        Block block = event.getBlockPlaced();
        ItemStack itemStack = player.getItemInHand();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);

        switch (xMaterial) {
            case PLAYER_HEAD:
                boolean hadToDisablePhysics = false;
                if (!buildWorld.isPhysics()) {
                    hadToDisablePhysics = true;
                    buildWorld.setPhysics(true);
                }

                plugin.getCustomBlocks().setBlock(event,
                        plugin.getString("blocks_full_oak_barch"),
                        plugin.getString("blocks_full_spruce_barch"),
                        plugin.getString("blocks_full_birch_barch"),
                        plugin.getString("blocks_full_jungle_barch"),
                        plugin.getString("blocks_full_acacia_barch"),
                        plugin.getString("blocks_full_dark_oak_barch"),
                        plugin.getString("blocks_red_mushroom"),
                        plugin.getString("blocks_brown_mushroom"),
                        plugin.getString("blocks_full_mushroom_stem"),
                        plugin.getString("blocks_mushroom_stem"),
                        plugin.getString("blocks_mushroom_block"),
                        plugin.getString("blocks_smooth_stone"),
                        plugin.getString("blocks_double_stone_slab"),
                        plugin.getString("blocks_smooth_sandstone"),
                        plugin.getString("blocks_smooth_red_sandstone"),
                        plugin.getString("blocks_powered_redstone_lamp"),
                        plugin.getString("blocks_burning_furnace"),
                        plugin.getString("blocks_command_block"),
                        plugin.getString("blocks_barrier"),
                        plugin.getString("blocks_mob_spawner"),
                        plugin.getString("blocks_nether_portal"),
                        plugin.getString("blocks_end_portal"),
                        plugin.getString("blocks_dragon_egg"));

                if (hadToDisablePhysics) buildWorld.setPhysics(false);
                break;
            /*
            case SPONGE:
                if (!world.isPhysics()) { // Stop sponge from soaking up water
                    event.setCancelled(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(itemStack.getType(), false),1L);
                }
                break;
             */
        }
    }

    private void disableArchivedWorlds(BuildWorld buildWorld, Player player, BlockPlaceEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(BuildWorld buildWorld, Player player, BlockPlaceEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!buildWorld.isBlockPlacement() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, BlockPlaceEvent event) {
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
