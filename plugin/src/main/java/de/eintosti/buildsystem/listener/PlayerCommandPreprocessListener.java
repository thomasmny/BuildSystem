package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;

/**
 * @author einTosti
 */
public class PlayerCommandPreprocessListener implements Listener {
    private static final List<String> DISABLED_COMMANDS = Arrays.asList(
            "/worldedit",
            "/br",
            "/ascend",
            "/biome",
            "/butcher",
            "/ceil",
            "/cs",
            "/cycler",
            "/delchunks",
            "/deltree",
            "/descend",
            "/farwand",
            "/floodfill",
            "/forestgen",
            "/mat",
            "/pumpkins",
            "/remove",
            "/repl",
            "/schem",
            "/tool",
            "/tree",
            "//"
    );
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public PlayerCommandPreprocessListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        if (!plugin.isBlockWorldEditNonBuilder()) return;

        Player player = event.getPlayer();

        boolean found = DISABLED_COMMANDS.stream().anyMatch(s -> event.getMessage()
                .startsWith(s));
        if (!found) return;

        World world = worldManager.getWorld(player.getWorld().getName());
        if (world == null) return;

        if (disableArchivedWorlds(world, player, event)) return;
        checkBuilders(world, player, event);
    }

    private boolean disableArchivedWorlds(World world, Player player, PlayerCommandPreprocessEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive"))
            return false;
        if (world.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getString("command_archive_world"));
            return true;
        }
        return false;
    }

    private void checkBuilders(World world, Player player, PlayerCommandPreprocessEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && world.getCreatorId() != null && world.getCreatorId()
                .equals(player.getUniqueId())) {
            return;
        }
        if (world.isBuilders() && !world.isBuilder(player)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getString("command_not_builder"));
        }
    }
}
