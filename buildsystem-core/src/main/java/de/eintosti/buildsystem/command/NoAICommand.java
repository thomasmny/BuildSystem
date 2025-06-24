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
import de.eintosti.buildsystem.util.EntityAIManager;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NoAICommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;

    public NoAICommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getCommand("noai").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, "buildsystem.noai")) {
            Messages.sendPermissionError(player);
            return true;
        }

        switch (args.length) {
            case 0:
                toggleAI(player, player.getWorld());
                break;
            case 1:
                toggleAI(player, Bukkit.getWorld(args[0]));
                break;
            default:
                Messages.sendMessage(player, "noai_usage");
                break;
        }

        return true;
    }

    private void toggleAI(Player player, World bukkitWorld) {
        if (bukkitWorld == null) {
            Messages.sendMessage(player, "noai_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(bukkitWorld);
        if (buildWorld == null) {
            Messages.sendMessage(player, "noai_world_not_imported");
            return;
        }

        WorldData worldData = buildWorld.getData();
        if (worldData.mobAi().get()) {
            worldData.mobAi().set(false);
            Messages.sendMessage(player, "noai_activated", Map.entry("%world%", buildWorld.getName()));
        } else {
            worldData.mobAi().set(true);
            Messages.sendMessage(player, "noai_deactivated", Map.entry("%world%", buildWorld.getName()));
        }

        boolean mobAI = worldData.mobAi().get();
        for (Entity entity : bukkitWorld.getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                EntityAIManager.setAIEnabled(livingEntity, mobAI);
            }
        }
    }
}