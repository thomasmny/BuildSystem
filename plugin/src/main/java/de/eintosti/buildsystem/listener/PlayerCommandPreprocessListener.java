package de.eintosti.buildsystem.listener;

import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;

/**
 * @author einTosti
 */
public class PlayerCommandPreprocessListener implements Listener {
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

        String command = "/" + event.getMessage().split(" ")[0];
        boolean found = DISABLED_COMMANDS.contains(command);
        if (!found) return;

        Player player = event.getPlayer();
        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) return;

        if (disableArchivedWorlds(buildWorld, player, event)) return;
        checkBuilders(buildWorld, player, event);
    }

    private final static HashSet<String> DISABLED_COMMANDS = Sets.newHashSet(
            "worldedit",

            // History Control
            "/undo",
            "/redo",
            "clearhistory",

            // Region Selection
            "/wand",
            "toggleeditwand",
            "/sel",
            "/desel",
            "/pos1",
            "/pos2",
            "/hpos1",
            "/hpos2",
            "/chunk",
            "/expand",
            "/contract",
            "/outset",
            "/inset",
            "/count",
            "/distr",

            // Region Operation
            "/set",
            "/replace",
            "/repl",
            "/overlay",
            "/walls",
            "/outline",
            "/center",
            "/smooth",
            "/deform",
            "/regen",
            "/hollow",
            "/move",
            "/stack",
            "/naturalize",
            "/line",
            "/curve",
            "/forest",
            "/flora",

            // Clipboards and Schematics
            "/copy",
            "/cut",
            "/paste",
            "/rotate",
            "/flip",
            "/schematic",
            "/schem",
            "clearclipboard",

            // Generation
            "/generate",
            "/generatebiome",
            "/hcyl",
            "/cyl",
            "/sphere",
            "/hsphere",
            "/pyramid",
            "forestgen",
            "pumpkins",

            // Utilities
            "toggleplace",
            "/fill",
            "/fillr",
            "/drain",
            "/fixwater",
            "/fixlava",
            "removeabove",
            "removebelow",
            "replacenear",
            "removenear",
            "snow",
            "thaw",
            "ex",
            "butcher",
            "remove",
            "green",
            "/calc",

            // Chunk Tools
            "chunkinfo",
            "listchunks",
            "delchunks",

            // Superpickaxe Tools
            "/",
            "sp single",
            "sp area",
            "sp recur",

            // General Tools
            "tool",
            "none",
            "farwand",
            "lrbuild",
            "tree",
            "deltree",
            "repl",
            "cycler",
            "flood",

            // Brushes
            "brush",
            "size",
            "mat",
            "range",
            "mask",
            "/gmask",

            // Quick-Travel
            "unstuck",
            "ascend",
            "descend",
            "thru",
            "jumpto",
            "up",

            // Snapshots
            "/restore",
            "snapshot",

            // Java Scriptings
            "/cs",
            ".s",

            // Biomes
            "biomelist",
            "biomeinfo",
            "/setbiome"
    );

    private boolean disableArchivedWorlds(BuildWorld buildWorld, Player player, PlayerCommandPreprocessEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) {
            return false;
        }

        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getString("command_archive_world"));
            return true;
        }
        return false;
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, PlayerCommandPreprocessEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getString("command_not_builder"));
        }
    }
}
