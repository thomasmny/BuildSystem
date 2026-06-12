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
import de.eintosti.buildsystem.api.event.world.PlayerBuildModeToggleEvent;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.api.player.PlayerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildCommand extends CommandBase {

    private final PlayerService playerService;

    public BuildCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
        this.playerService = plugin.getPlayerService();
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, "buildsystem.build")) {
            return;
        }

        switch (args.length) {
            case 0 -> toggleBuildMode(player, player);

            case 1 -> {
                if (!requirePermission(player, "buildsystem.build.other")) {
                    return;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    messages.sendMessage(player, "build_player_not_found");
                    return;
                }

                toggleBuildMode(target, player);
            }

            default -> messages.sendMessage(player, "build_usage");
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (!player.hasPermission("buildsystem.build")) {
            return list;
        }

        if (args.length == 1 && !player.hasPermission("buildsystem.build.other")) {
            Bukkit.getOnlinePlayers().forEach(pl -> addArgument(args[0], pl.getName(), list));
        }

        return list;
    }

    private void toggleBuildMode(Player target, Player sender) {
        UUID targetUuid = target.getUniqueId();
        boolean isEnteringBuildMode = !playerService.getBuildModePlayers().contains(targetUuid);

        PlayerBuildModeToggleEvent toggleEvent = new PlayerBuildModeToggleEvent(target, isEnteringBuildMode, sender);
        Bukkit.getServer().getPluginManager().callEvent(toggleEvent);
        if (toggleEvent.isCancelled()) {
            return;
        }

        BuildPlayerImpl buildPlayer = BuildPlayerImpl.of(playerService.getPlayerStorage().getBuildPlayer(target));
        CachedValues cachedValues = buildPlayer.getCachedValues();

        if (isEnteringBuildMode) {
            playerService.getBuildModePlayers().add(targetUuid);
            cachedValues.saveGameMode(target.getGameMode());
            cachedValues.saveInventory(target.getInventory().getContents());
            target.setGameMode(GameMode.CREATIVE);

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (sender.equals(target)) {
                messages.sendMessage(target, "build_activated_self");
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                messages.sendMessage(sender, "build_activated_other_sender", Map.entry("%target%", target.getName()));
                messages.sendMessage(target, "build_activated_other_target", Map.entry("%sender%", sender.getName()));
            }
        } else {
            if (!playerService.getBuildModePlayers().remove(targetUuid)) {
                return;
            }

            cachedValues.resetGameModeIfPresent(target);
            cachedValues.resetInventoryIfPresent(target);

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (sender.equals(target)) {
                messages.sendMessage(target, "build_deactivated_self");
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                messages.sendMessage(sender, "build_deactivated_other_sender", Map.entry("%target%", target.getName()));
                messages.sendMessage(target, "build_deactivated_other_target", Map.entry("%sender%", sender.getName()));
            }
        }
    }
}
