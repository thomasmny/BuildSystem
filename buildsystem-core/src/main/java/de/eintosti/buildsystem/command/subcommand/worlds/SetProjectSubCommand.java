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
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SetProjectSubCommand extends AbstractSubCommand {

    private final Menus menus;
    private final Prompts prompts;
    private final SettingsService settingsService;

    public SetProjectSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            Menus menus,
            Prompts prompts,
            SettingsService settingsService) {
        super(messages, worldService);
        this.menus = menus;
        this.prompts = prompts;
        this.settingsService = settingsService;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 2, "worlds_setproject");
        if (buildWorld == null) {
            return;
        }

        getProjectInput(player, buildWorld, true);
    }

    public void getProjectInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        prompts.prompt(player).title("enter_world_project").request(input -> {
            buildWorld.getData().setProject(input.trim());
            settingsService.forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            messages.sendMessage(player, "worlds_setproject_set", Map.entry("%world%", buildWorld.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                menus.openEdit(buildWorld, player);
            }
        });
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        WorldStorage ws = worldService.getWorldStorage();
        return WorldsCompletions.permittedWorldNames(player, ws, getArgument().getPermission(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.SET_PROJECT;
    }
}
