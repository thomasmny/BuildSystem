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

import de.eintosti.buildsystem.command.HelpPages.Entry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildSystemCommand extends CommandBase {

    private static final List<Entry> COMMANDS = List.of(
            new Entry("/back", "buildsystem_back", "/back", Permissions.BACK),
            new Entry("/blocks", "buildsystem_blocks", "/blocks", Permissions.BLOCKS),
            new Entry("/build [player]", "buildsystem_build", "/build", Permissions.BUILD),
            new Entry("/config reload", "buildsystem_config", "/config reload", Permissions.CONFIG),
            new Entry("/day [world]", "buildsystem_day", "/day", Permissions.DAY),
            new Entry("/explosions [world]", "buildsystem_explosions", "/explosions", Permissions.EXPLOSIONS),
            new Entry("/gm <gamemode> [player]", "buildsystem_gamemode", "/gm ", Permissions.GAMEMODE),
            new Entry("/night [world]", "buildsystem_night", "/night", Permissions.NIGHT),
            new Entry("/noai [world]", "buildsystem_noai", "/noai", Permissions.NOAI),
            new Entry("/physics [world]", "buildsystem_physics", "/physics", Permissions.PHYSICS),
            new Entry("/settings", "buildsystem_settings", "/settings", Permissions.SETTINGS),
            new Entry("/setup", "buildsystem_setup", "/setup", Permissions.SETUP),
            new Entry("/skull [player/id]", "buildsystem_skull", "/skull", Permissions.SKULL),
            new Entry("/spawn", "buildsystem_spawn", "/spawn", "-"),
            new Entry("/speed <1-5>", "buildsystem_speed", "/speed ", Permissions.SPEED),
            new Entry("/top", "buildsystem_top", "/top", Permissions.TOP),
            new Entry("/worlds help", "buildsystem_worlds", "/worlds help", "-"));

    private final HelpPages pages;

    public BuildSystemCommand(Messages messages, Logger logger) {
        super(messages, logger, true);
        this.pages = new HelpPages(messages, "buildsystem", COMMANDS);
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, Permissions.HELP_BUILDSYSTEM)) {
            return;
        }

        switch (args.length) {
            case 0 -> pages.send(player, 1);
            case 1 -> pages.send(player, args[0]);
            default -> messages.sendMessage(player, "buildsystem_usage");
        }
    }
}
