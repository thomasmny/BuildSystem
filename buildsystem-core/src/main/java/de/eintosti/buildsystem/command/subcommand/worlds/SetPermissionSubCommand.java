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
import de.eintosti.buildsystem.config.ConfigService;
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
public class SetPermissionSubCommand extends AbstractSubCommand {

    private final ConfigService configService;
    private final Menus menus;
    private final Prompts prompts;
    private final SettingsService settingsService;

    public SetPermissionSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            ConfigService configService,
            Menus menus,
            Prompts prompts,
            SettingsService settingsService) {
        super(messages, worldService);
        this.configService = configService;
        this.menus = menus;
        this.prompts = prompts;
        this.settingsService = settingsService;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 2, "worlds_setpermission");
        if (buildWorld == null) {
            return;
        }

        getPermissionInput(player, buildWorld, true);
    }

    public void getPermissionInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        prompts.prompt(player).title("enter_world_permission").request(input -> {
            String permission = input.trim();

            List<String> whitelist = configService.current().settings().worldPermissionWhitelist();
            if (!isPermissionAllowed(permission, whitelist)) {
                XSound.ENTITY_ITEM_BREAK.play(player);
                messages.sendMessage(player, "worlds_setpermission_not_allowed");

                if (closeInventory) {
                    player.closeInventory();
                } else {
                    menus.openEdit(buildWorld, player);
                }
                return;
            }

            buildWorld.getData().setPermission(permission);
            settingsService.forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            messages.sendMessage(player, "worlds_setpermission_set", Map.entry("%world%", buildWorld.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                menus.openEdit(buildWorld, player);
            }
        });
    }

    /**
     * Determines whether a permission lock may be set via {@code /worlds setPermission}.
     *
     * <p>An empty whitelist imposes no restriction (today's behavior). When the whitelist is non-empty, only listed
     * values and the {@code "-"} sentinel (which clears the permission) are allowed.
     *
     * @param input The trimmed permission input
     * @param whitelist The configured whitelist of permitted permission strings
     * @return {@code true} if the permission may be set, {@code false} otherwise
     */
    static boolean isPermissionAllowed(String input, List<String> whitelist) {
        return whitelist.isEmpty() || input.equals("-") || whitelist.contains(input);
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
        return WorldsArgument.SET_PERMISSION;
    }
}
