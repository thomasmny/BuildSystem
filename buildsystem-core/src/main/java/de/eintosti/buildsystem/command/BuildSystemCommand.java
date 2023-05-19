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
            plugin.getLogger().warning(Messages.getString("sender_not_player"));
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
    protected List<TextComponent> getCommands() {
        List<TextComponent> commands = Lists.newArrayList(
                createComponent("/back", Messages.getString("buildsystem_back"), "/back", "buildsystem.back"),
                createComponent("/blocks", Messages.getString("buildsystem_blocks"), "/blocks", "buildsystem.blocks"),
                createComponent("/build [player]", Messages.getString("buildsystem_build"), "/build", "buildsystem.build"),
                createComponent("/config reload", Messages.getString("buildsystem_config"), "/config reload", "buildsystem.config"),
                createComponent("/day [world]", Messages.getString("buildsystem_day"), "/day", "buildsystem.day"),
                createComponent("/explosions [world]", Messages.getString("buildsystem_explosions"), "/explosions", "buildsystem.explosions"),
                createComponent("/gm <gamemode> [player]", Messages.getString("buildsystem_gamemode"), "/gm ", "buildsystem.gamemode"),
                createComponent("/night [world]", Messages.getString("buildsystem_night"), "/night", "buildsystem.night"),
                createComponent("/noai [world]", Messages.getString("buildsystem_noai"), "/noai", "buildsystem.noai"),
                createComponent("/physics [world]", Messages.getString("buildsystem_physics"), "/physics", "buildsystem.physics"),
                createComponent("/settings", Messages.getString("buildsystem_settings"), "/settings", "buildsystem.settings"),
                createComponent("/setup", Messages.getString("buildsystem_setup"), "/setup", "buildsystem.setup"),
                createComponent("/skull [player/id]", Messages.getString("buildsystem_skull"), "/skull", "buildsystem.skull"),
                createComponent("/spawn", Messages.getString("buildsystem_spawn"), "/spawn", "-"),
                createComponent("/speed <1-5>", Messages.getString("buildsystem_speed"), "/speed ", "buildsystem.speed"),
                createComponent("/top", Messages.getString("buildsystem_top"), "/top", "buildsystem.top"),
                createComponent("/worlds help", Messages.getString("buildsystem_worlds"), "/worlds help", "-")
        );
        commands.removeIf(textComponent -> textComponent.getText().isEmpty());
        return commands;
    }
}