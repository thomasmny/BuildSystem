/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

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
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        sendCommandMessage(player);
        return true;
    }

    private void sendCommandMessage(Player player) {
        TextComponent line1 = new TextComponent("§7§m----------------------------------------------------\n");
        TextComponent line2 = new TextComponent(plugin.getString("buildsystem_title") + "\n");
        TextComponent line3 = new TextComponent("§7 \n");
        TextComponent line4 = createComponent("/back", " §8» " + plugin.getString("buildsystem_back"), "/back", "buildsystem.back");
        TextComponent line5 = createComponent("/blocks", " §8» " + plugin.getString("buildsystem_blocks"), "/blocks", "buildsystem.blocks");
        TextComponent line6 = createComponent("/build [player]", " §8» " + plugin.getString("buildsystem_build"), "/build", "buildsystem.build");
        TextComponent line7 = createComponent("/config reload", " §8» " + plugin.getString("buildsystem_config"), "/config reload", "buildsystem.config");
        TextComponent line8 = createComponent("/day [world]", " §8» " + plugin.getString("buildsystem_day"), "/day", "buildsystem.day");
        TextComponent line9 = createComponent("/explosions [world]", " §8» " + plugin.getString("buildsystem_explosions"), "/explosions", "buildsystem.explosions");
        TextComponent line10 = createComponent("/gm <gamemode> [player]", " §8» " + plugin.getString("buildsystem_gamemode"), "/gm ", "buildsystem.gamemode");
        TextComponent line11 = createComponent("/night [world]", " §8» " + plugin.getString("buildsystem_night"), "/night", "buildsystem.night");
        TextComponent line12 = createComponent("/noai [world]", " §8» " + plugin.getString("buildsystem_noai"), "/noai", "buildsystem.noai");
        TextComponent line13 = createComponent("/physics [world]", " §8» " + plugin.getString("buildsystem_physics"), "/physics", "buildsystem.physics");
        TextComponent line14 = createComponent("/settings", " §8» " + plugin.getString("buildsystem_settings"), "/settings", "buildsystem.settings");
        TextComponent line15 = createComponent("/setup", " §8» " + plugin.getString("buildsystem_setup"), "/setup", "buildsystem.setup");
        TextComponent line16 = createComponent("/skull [player/id]", " §8» " + plugin.getString("buildsystem_skull"), "/skull", "buildsystem.skull");
        TextComponent line17 = createComponent("/spawn", " §8» " + plugin.getString("buildsystem_spawn"), "/spawn", "-");
        TextComponent line18 = createComponent("/speed <1-5>", " §8» " + plugin.getString("buildsystem_speed"), "/speed ", "buildsystem.speed");
        TextComponent line19 = createComponent("/top", " §8» " + plugin.getString("buildsystem_top"), "/top", "buildsystem.top");
        TextComponent line20 = createComponent("/worlds help", " §8» " + plugin.getString("buildsystem_worlds"), "/worlds help", "-");
        TextComponent line21 = new TextComponent("§7§m----------------------------------------------------");

        player.spigot().sendMessage(line1, line2, line3, line4, line5, line6, line7, line8, line9, line10, line11, line12, line13, line14, line15, line16, line17, line18, line19, line20, line21);
    }

    private TextComponent createComponent(String command, String text, String suggest, String permission) {
        TextComponent lineComponent = new TextComponent("§8 - ");
        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(text + "\n");

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(plugin.getString("worlds_help_permission").replace("%permission%", permission)).create()));

        commandComponent.addExtra(textComponent);
        lineComponent.addExtra(commandComponent);
        return lineComponent;
    }
}
