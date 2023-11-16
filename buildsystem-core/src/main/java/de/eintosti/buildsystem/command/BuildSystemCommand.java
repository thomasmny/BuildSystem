/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command;

import com.google.common.collect.Lists;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BuildSystemCommand extends PagedCommand implements CommandExecutor {

    private final BuildSystem plugin;

    public BuildSystemCommand(BuildSystem plugin) {
        super("buildsystem_permission", "buildsystem_title_with_page");

        this.plugin = plugin;
        plugin.getCommand("buildsystem").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendMessage(player, 1);
        } else if (args.length == 1) {
            try {
                int page = Integer.parseInt(args[0]);
                sendMessage(player, page);
            } catch (NumberFormatException e) {
                Messages.sendMessage(player, "buildsystem_invalid_page");
            }
        } else {
            Messages.sendMessage(player, "buildsystem_usage");
        }
        return true;
    }

    @Override
    protected List<TextComponent> getCommands(Player player) {
        List<TextComponent> commands = Lists.newArrayList(
                createComponent(player, "/back", "buildsystem_back", "/back", "buildsystem.back"),
                createComponent(player, "/blocks", "buildsystem_blocks", "/blocks", "buildsystem.blocks"),
                createComponent(player, "/build [player]", "buildsystem_build", "/build", "buildsystem.build"),
                createComponent(player, "/config reload", "buildsystem_config", "/config reload", "buildsystem.config"),
                createComponent(player, "/day [world]", "buildsystem_day", "/day", "buildsystem.day"),
                createComponent(player, "/explosions [world]", "buildsystem_explosions", "/explosions", "buildsystem.explosions"),
                createComponent(player, "/gm <gamemode> [player]", "buildsystem_gamemode", "/gm ", "buildsystem.gamemode"),
                createComponent(player, "/night [world]", "buildsystem_night", "/night", "buildsystem.night"),
                createComponent(player, "/noai [world]", "buildsystem_noai", "/noai", "buildsystem.noai"),
                createComponent(player, "/physics [world]", "buildsystem_physics", "/physics", "buildsystem.physics"),
                createComponent(player, "/settings", "buildsystem_settings", "/settings", "buildsystem.settings"),
                createComponent(player, "/setup", "buildsystem_setup", "/setup", "buildsystem.setup"),
                createComponent(player, "/skull [player/id]", "buildsystem_skull", "/skull", "buildsystem.skull"),
                createComponent(player, "/spawn", "buildsystem_spawn", "/spawn", "-"),
                createComponent(player, "/speed <1-5>", "buildsystem_speed", "/speed ", "buildsystem.speed"),
                createComponent(player, "/top", "buildsystem_top", "/top", "buildsystem.top"),
                createComponent(player, "/worlds help", "buildsystem_worlds", "/worlds help", "-")
        );
        commands.removeIf(textComponent -> textComponent.getText().isEmpty());
        return commands;
    }
}