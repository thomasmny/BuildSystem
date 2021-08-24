package de.eintosti.buildsystem.listener;

import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.event.PlayerInventoryClearEvent;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;

/**
 * @author einTosti
 */
public class PlayerCommandPreprocessListener implements Listener {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    public PlayerCommandPreprocessListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String command = event.getMessage().split(" ")[0];
        Player player = event.getPlayer();

        if (command.equalsIgnoreCase("/clear")) {
            ItemStack navigatorItem = inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item"));
            if (!player.getInventory().contains(navigatorItem)) return;

            if (settingsManager.getSettings(player).isKeepNavigator()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    PlayerInventoryClearEvent playerInventoryClearEvent = new PlayerInventoryClearEvent(player);
                    Bukkit.getServer().getPluginManager().callEvent(playerInventoryClearEvent);
                }, 1L);
            }
            return;
        }

        if (plugin.isBlockWorldEditNonBuilder()) {
            if (!DISABLED_COMMANDS.contains(command)) return;

            BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
            if (buildWorld == null) {
                return;
            }

            if (disableArchivedWorlds(buildWorld, player, event)) {
                return;
            }

            checkBuilders(buildWorld, player, event);
        }
    }

    private final static HashSet<String> DISABLED_COMMANDS = Sets.newHashSet(
            "/worldedit",
            "/we",

            // History Control
            "//undo",
            "//redo",
            "/clearhistory",

            // Region Selection
            "//wand",
            "/toggleeditwand",
            "//sel",
            "//desel",
            "//pos1",
            "//pos2",
            "//0",
            "//1",
            "//2",
            "//hpos1",
            "//hpos2",
            "//chunk",
            "//expand",
            "//contract",
            "//outset",
            "//inset",
            "//count",
            "//distr",

            // Region Operation
            "//set",
            "//replace",
            "//repl",
            "//overlay",
            "//walls",
            "//outline",
            "//center",
            "//smooth",
            "//deform",
            "//regen",
            "//hollow",
            "//move",
            "//stack",
            "//naturalize",
            "//line",
            "//curve",
            "//forest",
            "//flora",
            "//air",

            // Clipboards and Schematics
            "//copy",
            "//cut",
            "//paste",
            "//rotate",
            "//flip",
            "//schematic",
            "//schem",
            "/clearclipboard",

            // Generation
            "//generate",
            "//generatebiome",
            "//hcyl",
            "//cyl",
            "//sphere",
            "//hsphere",
            "//pyramid",
            "/forestgen",
            "/pumpkins",

            // Utilities
            "/toggleplace",
            "//fill",
            "//fillr",
            "//drain",
            "//fixwater",
            "//fixlava",
            "/removeabove",
            "/removebelow",
            "/replacenear",
            "/removenear",
            "/snow",
            "/thaw",
            "/ex",
            "/butcher",
            "/remove",
            "/green",
            "//calc",

            // Chunk Tools
            "/chunkinfo",
            "/listchunks",
            "/delchunks",

            // Superpickaxe Tools
            "//",
            "/sp single",
            "/sp area",
            "/sp recur",

            // General Tools
            "/tool",
            "/none",
            "/farwand",
            "/lrbuild",
            "/tree",
            "/deltree",
            "/repl",
            "/cycler",
            "/flood",

            // Brushes
            "//brush",
            "//br",
            "/brush",
            "/br",
            "/size",
            "/mat",
            "/range",
            "/mask",
            "//gmask",

            // Quick-Travel
            "/unstuck",
            "/ascend",
            "/asc",
            "/descend",
            "/desc",
            "/thru",
            "/jumpto",
            "/up",

            // Snapshots
            "//restore",
            "/snapshot",

            // Java Scriptings
            "//cs",
            "/.s",

            // Biomes
            "/biomelist",
            "/biomels",
            "/biomeinfo",
            "//setbiome"
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
