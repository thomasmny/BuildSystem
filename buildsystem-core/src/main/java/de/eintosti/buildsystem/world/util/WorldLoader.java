/*
 * Copyright (c) 2018-2025, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.world.util;

import com.cryptomorin.xseries.messages.Titles;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.event.world.BuildWorldLoadEvent;
import de.eintosti.buildsystem.event.world.BuildWorldPostLoadEvent;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.BuildWorldCreator;
import java.util.AbstractMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldLoader {

    private final BuildSystem plugin;
    private final BuildWorld buildWorld;

    public WorldLoader(BuildWorld buildWorld) {
        this.plugin = JavaPlugin.getPlugin(BuildSystem.class);
        this.buildWorld = buildWorld;
    }

    public void loadForPlayer(Player player) {
        if (buildWorld.isLoaded()) {
            return;
        }

        player.closeInventory();
        Titles.sendTitle(player, 5, 70, 20, " ",
                Messages.getString("loading_world", player,
                        new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()))
        );

        load();
    }

    public void load() {
        if (buildWorld.isLoaded()) {
            return;
        }

        BuildWorldLoadEvent loadEvent = new BuildWorldLoadEvent(buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(loadEvent);
        if (loadEvent.isCancelled()) {
            return;
        }

        plugin.getLogger().info("*** Loading world \"" + buildWorld.getName() + "\" ***");
        World world = new BuildWorldCreator(plugin, buildWorld).generateBukkitWorld();
        if (world == null) {
            return;
        }

        buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        buildWorld.setLoaded(true);

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostLoadEvent(buildWorld));
        buildWorld.resetUnloadTask();
    }
} 