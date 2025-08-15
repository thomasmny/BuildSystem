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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.event.world.BuildWorldLoadEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldPostLoadEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.util.WorldLoader;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldLoaderImpl implements WorldLoader {

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    private WorldLoaderImpl(BuildWorld buildWorld) {
        this.plugin = BuildSystemPlugin.get();
        this.buildWorld = buildWorld;
    }

    @Contract("_ -> new")
    public static WorldLoaderImpl of(BuildWorld buildWorld) {
        return new WorldLoaderImpl(buildWorld);
    }

    @Override
    public void loadForPlayer(Player player) {
        if (this.buildWorld.isLoaded()) {
            return;
        }

        player.closeInventory();
        player.sendTitle(" ", Messages.getString("loading_world", player, Map.entry("%world%", this.buildWorld.getName())), 5, 70, 20);

        load();
    }

    @Override
    public void load() {
        if (this.buildWorld.isLoaded()) {
            return;
        }

        BuildWorldLoadEvent loadEvent = new BuildWorldLoadEvent(this.buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(loadEvent);
        if (loadEvent.isCancelled()) {
            return;
        }

        String worldName = this.buildWorld.getName();
        this.plugin.getLogger().info("*** Loading world \"" + worldName + "\" ***");
        World world = new BuildWorldCreatorImpl(this.plugin, this.buildWorld).generateBukkitWorld();
        if (world == null) {
            return;
        }

        this.buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        this.buildWorld.setLoaded(true);

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostLoadEvent(this.buildWorld));
        this.buildWorld.getUnloader().resetUnloadTask();
    }
} 