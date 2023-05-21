/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.event.player.PlayerInventoryClearEvent;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import de.eintosti.buildsystem.world.data.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class PlayerCommandPreprocessListener implements Listener {

    private static final Set<String> DISABLED_COMMANDS = Sets.newHashSet(
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
            "//setbiome",

            // Voxel Sniper
            "/vs",
            "/voxel",
            "/voxel_chunk",
            "/voxel_height",
            "/voxel_ink",
            "/voxel_ink_replace",
            "/voxel_list",
            "/voxel_replace",
            "/voxel_sniper",
            "/b",
            "/brush",
            "/brush_toolkit",
            "/d",
            "/default",
            "/goto",
            "/p",
            "/paint",
            "/perf",
            "/performer",
            "/v",
            "/vc",
            "/vchunk",
            "/vh",
            "/vi",
            "/vir",
            "/vl",
            "/vr--"
    );

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final InventoryUtils inventoryUtils;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    public PlayerCommandPreprocessListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.inventoryUtils = plugin.getInventoryUtil();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String command = event.getMessage().split(" ")[0];
        Player player = event.getPlayer();

        if (command.equalsIgnoreCase("/clear")) {
            ItemStack navigatorItem = inventoryUtils.getItemStack(configValues.getNavigatorItem(), Messages.getString("navigator_item"));
            if (!player.getInventory().contains(navigatorItem)) {
                return;
            }

            if (settingsManager.getSettings(player).isKeepNavigator()) {
                List<Integer> navigatorSlots = inventoryUtils.getNavigatorSlots(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    PlayerInventoryClearEvent playerInventoryClearEvent = new PlayerInventoryClearEvent(player, navigatorSlots);
                    Bukkit.getServer().getPluginManager().callEvent(playerInventoryClearEvent);
                }, 2L);
            }
            return;
        }

        if (configValues.isBlockWorldEditNonBuilder()) {
            if (!DISABLED_COMMANDS.contains(command)) {
                return;
            }

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

    private boolean disableArchivedWorlds(BuildWorld buildWorld, Player player, PlayerCommandPreprocessEvent event) {
        if (!worldManager.canBypassBuildRestriction(player) && buildWorld.getData().status().get() == WorldStatus.ARCHIVE) {
            event.setCancelled(true);
            Messages.sendMessage(player, "command_archive_world");
            return true;
        }
        return false;
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, PlayerCommandPreprocessEvent event) {
        if (worldManager.canBypassBuildRestriction(player)) {
            return;
        }

        if (buildWorld.isCreator(player)) {
            return;
        }

        if (buildWorld.getData().buildersEnabled().get() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
            Messages.sendMessage(player, "command_not_builder");
        }
    }
}