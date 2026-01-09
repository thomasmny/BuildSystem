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
package de.eintosti.buildsystem.command;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.event.world.PlayerBuildModeToggleEvent;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.CachedValues;
import de.eintosti.buildsystem.api.player.PlayerService;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;
    private final PlayerService playerService;

    public BuildCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerService = plugin.getPlayerService();
        plugin.getCommand("build").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", sender));
            return true;
        }

        if (!player.hasPermission("buildsystem.build")) {
            Messages.sendPermissionError(player);
            return true;
        }

        switch (args.length) {
            case 0 -> {
                toggleBuildMode(player, player);
            }

            case 1 -> {
                if (!player.hasPermission("buildsystem.build.other")) {
                    Messages.sendPermissionError(player);
                    return true;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    Messages.sendMessage(player, "build_player_not_found");
                    return true;
                }

                toggleBuildMode(target, player);
            }

            default -> {
                Messages.sendMessage(player, "build_usage");
            }
        }

        return true;
    }

    /**
     * Toggles the build mode for a target player.
     *
     * @param target The player whose build mode is being changed
     * @param sender The player who initiated the action, may be the target player themselves
     */
    private void toggleBuildMode(Player target, Player sender) {
        UUID targetUuid = target.getUniqueId();
        boolean isEnteringBuildMode = !playerService.getBuildModePlayers().contains(targetUuid);

        PlayerBuildModeToggleEvent toggleEvent = new PlayerBuildModeToggleEvent(target, isEnteringBuildMode, sender);
        Bukkit.getServer().getPluginManager().callEvent(toggleEvent);
        if (toggleEvent.isCancelled()) {
            return;
        }

        BuildPlayer buildPlayer = playerService.getPlayerStorage().getBuildPlayer(target);
        CachedValues cachedValues = buildPlayer.getCachedValues();

        if (isEnteringBuildMode) {
            playerService.getBuildModePlayers().add(targetUuid);
            cachedValues.saveGameMode(target.getGameMode());
            cachedValues.saveInventory(target.getInventory().getContents());
            target.setGameMode(GameMode.CREATIVE);

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (sender.equals(target)) {
                Messages.sendMessage(target, "build_activated_self");
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                Messages.sendMessage(sender, "build_activated_other_sender", Map.entry("%target%", target.getName()));
                Messages.sendMessage(target, "build_activated_other_target", Map.entry("%sender%", sender.getName()));
            }
        } else {
            if (!playerService.getBuildModePlayers().remove(targetUuid)) {
                return;
            }

            cachedValues.resetGameModeIfPresent(target);
            cachedValues.resetInventoryIfPresent(target);

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (sender.equals(target)) {
                Messages.sendMessage(target, "build_deactivated_self");
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                Messages.sendMessage(sender, "build_deactivated_other_sender", Map.entry("%target%", target.getName()));
                Messages.sendMessage(target, "build_deactivated_other_target", Map.entry("%sender%", sender.getName()));
            }
        }
    }
}