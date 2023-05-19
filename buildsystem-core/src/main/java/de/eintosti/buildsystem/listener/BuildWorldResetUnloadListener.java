package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BuildWorldResetUnloadListener implements Listener {

    private final WorldManager worldManager;

    public BuildWorldResetUnloadListener(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        World from = event.getFrom();
        BuildWorld buildWorld = worldManager.getBuildWorld(from.getName());
        if (buildWorld != null) {
            buildWorld.resetUnloadTask();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        World from = event.getPlayer().getWorld();
        BuildWorld buildWorld = worldManager.getBuildWorld(from.getName());
        if (buildWorld != null) {
            buildWorld.resetUnloadTask();
        }
    }
}