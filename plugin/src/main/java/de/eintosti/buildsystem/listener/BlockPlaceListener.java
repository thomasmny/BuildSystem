package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
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
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    public BlockPlaceListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onCustomBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        World world = worldManager.getWorld(worldName);

        if (world == null) return;
        disableArchivedWorlds(world, player, event);
        checkWorldSettings(world, player, event);
        checkBuilders(world, player, event);
        setStatus(world, player);

        ItemStack itemStack = player.getItemInHand();
        if (!itemStack.hasItemMeta()) return;

        if (itemStack.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
            boolean hadToDisablePhysics = false;
            if (!world.isPhysics()) {
                hadToDisablePhysics = true;
                world.setPhysics(true);
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

            if (hadToDisablePhysics) world.setPhysics(false);
        }
    }

    private void disableArchivedWorlds(World world, Player player, BlockPlaceEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (world.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(World world, Player player, BlockPlaceEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!world.isBlockPlacement() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void checkBuilders(World world, Player player, BlockPlaceEvent event) {
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
