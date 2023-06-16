package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.world.BuildWorldManager;
import de.eintosti.buildsystem.world.CraftBuildWorld;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BuildWorldResetUnloadListener implements Listener {

    private final BuildWorldManager worldManager;

    public BuildWorldResetUnloadListener(BuildSystemPlugin plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        CraftBuildWorld buildWorld = worldManager.getBuildWorld(from.getName());
        if (buildWorld != null) {
            buildWorld.resetUnloadTask();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        World from = event.getPlayer().getWorld();
        CraftBuildWorld buildWorld = worldManager.getBuildWorld(from.getName());
        if (buildWorld != null) {
            buildWorld.resetUnloadTask();
        }
    }
}