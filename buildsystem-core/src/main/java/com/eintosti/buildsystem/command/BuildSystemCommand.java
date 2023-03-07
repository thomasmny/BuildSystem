/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;

/**
 * @author einTosti
 */
public class BuildSystemCommand implements CommandExecutor {

    private final BuildSystem plugin;

    public BuildSystemCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("buildsystem").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player"));
            return true;
        }

        sendCommandMessage((Player) sender);
        return true;
    }

    private void sendCommandMessage(Player player) {
        TextComponent line1 = new TextComponent("§7§m----------------------------------------------------\n");
        TextComponent line2 = new TextComponent(Messages.getString("buildsystem_title") + "\n");
        TextComponent line3 = new TextComponent("§7 \n");
        TextComponent line4 = createComponent("/back", Messages.getString("buildsystem_back"), "/back", "buildsystem.back");
        TextComponent line5 = createComponent("/blocks", Messages.getString("buildsystem_blocks"), "/blocks", "buildsystem.blocks");
        TextComponent line6 = createComponent("/build [player]", Messages.getString("buildsystem_build"), "/build", "buildsystem.build");
        TextComponent line7 = createComponent("/config reload", Messages.getString("buildsystem_config"), "/config reload", "buildsystem.config");
        TextComponent line8 = createComponent("/day [world]", Messages.getString("buildsystem_day"), "/day", "buildsystem.day");
        TextComponent line9 = createComponent("/explosions [world]", Messages.getString("buildsystem_explosions"), "/explosions", "buildsystem.explosions");
        TextComponent line10 = createComponent("/gm <gamemode> [player]", Messages.getString("buildsystem_gamemode"), "/gm ", "buildsystem.gamemode");
        TextComponent line11 = createComponent("/night [world]", Messages.getString("buildsystem_night"), "/night", "buildsystem.night");
        TextComponent line12 = createComponent("/noai [world]", Messages.getString("buildsystem_noai"), "/noai", "buildsystem.noai");
        TextComponent line13 = createComponent("/physics [world]", Messages.getString("buildsystem_physics"), "/physics", "buildsystem.physics");
        TextComponent line14 = createComponent("/settings", Messages.getString("buildsystem_settings"), "/settings", "buildsystem.settings");
        TextComponent line15 = createComponent("/setup", Messages.getString("buildsystem_setup"), "/setup", "buildsystem.setup");
        TextComponent line16 = createComponent("/skull [player/id]", Messages.getString("buildsystem_skull"), "/skull", "buildsystem.skull");
        TextComponent line17 = createComponent("/spawn", Messages.getString("buildsystem_spawn"), "/spawn", "-");
        TextComponent line18 = createComponent("/speed <1-5>", Messages.getString("buildsystem_speed"), "/speed ", "buildsystem.speed");
        TextComponent line19 = createComponent("/top", Messages.getString("buildsystem_top"), "/top", "buildsystem.top");
        TextComponent line20 = createComponent("/worlds help", Messages.getString("buildsystem_worlds"), "/worlds help", "-");
        TextComponent line21 = new TextComponent("§7§m----------------------------------------------------");

        player.spigot().sendMessage(line1, line2, line3, line4, line5, line6, line7, line8, line9, line10, line11, line12, line13, line14, line15, line16, line17, line18, line19, line20, line21);
    }

    private TextComponent createComponent(String command, String text, String suggest, String permission) {
        if (text.isEmpty()) {
            return new TextComponent();
        }

        TextComponent lineComponent = new TextComponent("§8 - ");
        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(" §8» " + text + "\n");

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                Messages.getString("worlds_help_permission", new AbstractMap.SimpleEntry<>("%permission%", permission))
        ).create()));

        commandComponent.addExtra(textComponent);
        lineComponent.addExtra(commandComponent);
        return lineComponent;
    }
}