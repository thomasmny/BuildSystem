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

import com.google.common.collect.Lists;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import java.util.logging.Logger;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildSystemCommand extends PagedCommand {

    public BuildSystemCommand(Messages messages, Logger logger) {
        super(messages, logger, "buildsystem_title_with_page", "buildsystem_permission");
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, Permissions.HELP_BUILDSYSTEM)) {
            return;
        }

        if (args.length == 0) {
            sendMessage(player, 1);
        } else if (args.length == 1) {
            try {
                int page = Integer.parseInt(args[0]);
                sendMessage(player, page);
            } catch (NumberFormatException e) {
                messages.sendMessage(player, "buildsystem_invalid_page");
            }
        } else {
            messages.sendMessage(player, "buildsystem_usage");
        }
    }

    @Override
    protected List<TextComponent> getCommands(Player player) {
        List<TextComponent> commands = Lists.newArrayList(
                createComponent(player, "/back", "buildsystem_back", "/back", Permissions.BACK),
                createComponent(player, "/blocks", "buildsystem_blocks", "/blocks", Permissions.BLOCKS),
                createComponent(player, "/build [player]", "buildsystem_build", "/build", Permissions.BUILD),
                createComponent(player, "/config reload", "buildsystem_config", "/config reload", Permissions.CONFIG),
                createComponent(player, "/day [world]", "buildsystem_day", "/day", Permissions.DAY),
                createComponent(
                        player, "/explosions [world]", "buildsystem_explosions", "/explosions", Permissions.EXPLOSIONS),
                createComponent(
                        player, "/gm <gamemode> [player]", "buildsystem_gamemode", "/gm ", Permissions.GAMEMODE),
                createComponent(player, "/night [world]", "buildsystem_night", "/night", Permissions.NIGHT),
                createComponent(player, "/noai [world]", "buildsystem_noai", "/noai", Permissions.NOAI),
                createComponent(player, "/physics [world]", "buildsystem_physics", "/physics", Permissions.PHYSICS),
                createComponent(player, "/settings", "buildsystem_settings", "/settings", Permissions.SETTINGS),
                createComponent(player, "/setup", "buildsystem_setup", "/setup", Permissions.SETUP),
                createComponent(player, "/skull [player/id]", "buildsystem_skull", "/skull", Permissions.SKULL),
                createComponent(player, "/spawn", "buildsystem_spawn", "/spawn", "-"),
                createComponent(player, "/speed <1-5>", "buildsystem_speed", "/speed ", Permissions.SPEED),
                createComponent(player, "/top", "buildsystem_top", "/top", Permissions.TOP),
                createComponent(player, "/worlds help", "buildsystem_worlds", "/worlds help", "-"));
        commands.removeIf(textComponent -> textComponent.getText().isEmpty());
        return commands;
    }
}
