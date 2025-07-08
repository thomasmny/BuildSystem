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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ExplosionsCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;

    public ExplosionsCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getCommand("explosions").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", sender));
            return true;
        }

        String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, "buildsystem.explosions")) {
            Messages.sendPermissionError(player);
            return true;
        }

        switch (args.length) {
            case 0 -> toggleExplosions(player, player.getWorld());
            case 1 -> toggleExplosions(player, Bukkit.getWorld(args[0]));
            default -> Messages.sendMessage(player, "explosions_usage");
        }

        return true;
    }

    private void toggleExplosions(Player player, @Nullable World world) {
        if (world == null) {
            Messages.sendMessage(player, "explosions_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        if (buildWorld == null) {
            Messages.sendMessage(player, "explosions_world_not_imported");
            return;
        }

        WorldData worldData = buildWorld.getData();
        if (!worldData.explosions().get()) {
            worldData.explosions().set(true);
            Messages.sendMessage(player, "explosions_activated", Map.entry("%world%", buildWorld.getName()));
        } else {
            worldData.explosions().set(false);
            Messages.sendMessage(player, "explosions_deactivated", Map.entry("%world%", buildWorld.getName()));
        }
    }
}