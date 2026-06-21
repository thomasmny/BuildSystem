/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.world.lifecycle;

import de.eintosti.buildsystem.api.event.world.BuildWorldLoadEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldPostLoadEvent;
import de.eintosti.buildsystem.api.world.lifecycle.WorldLoader;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.creation.BukkitWorldFactory;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldLoaderImpl implements WorldLoader {

    private final WorldContext context;
    private final BuildWorldImpl buildWorld;

    private WorldLoaderImpl(WorldContext context, BuildWorldImpl buildWorld) {
        this.context = context;
        this.buildWorld = buildWorld;
    }

    @Contract("_, _ -> new")
    public static WorldLoaderImpl of(WorldContext context, BuildWorldImpl buildWorld) {
        return new WorldLoaderImpl(context, buildWorld);
    }

    @Override
    public void loadForPlayer(Player player) {
        if (this.buildWorld.isLoaded()) {
            return;
        }

        player.closeInventory();
        player.sendTitle(
                " ",
                context.messages().getString("loading_world", player, Map.entry("%world%", this.buildWorld.getName())),
                5,
                70,
                20);

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
        this.context.logger().info("*** Loading world \"" + worldName + "\" ***");
        World world = new BukkitWorldFactory(this.context.configService(), this.context.logger(), this.buildWorld)
                .generate(BukkitWorldFactory.VersionCheck.REQUIRED);
        if (world == null) {
            return;
        }

        this.buildWorld.getData().setLastLoaded(System.currentTimeMillis());
        this.buildWorld.setLoaded(true);

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostLoadEvent(this.buildWorld));
        this.buildWorld.getUnloader().resetUnloadTask();
    }
}
