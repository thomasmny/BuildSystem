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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SetCreatorSubCommand extends WorldSubCommand {

    private final PlayerLookupService playerLookupService;
    private final Prompts prompts;
    private final SettingsService settingsService;

    public SetCreatorSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            PlayerLookupService playerLookupService,
            Prompts prompts,
            SettingsService settingsService) {
        super(messages, worldService, WorldTarget.argument(2, "worlds_setcreator"));
        this.playerLookupService = playerLookupService;
        this.prompts = prompts;
        this.settingsService = settingsService;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        prompts.prompt(player).title("enter_world_creator").request(input -> {
            String creatorName = input.trim();
            if (creatorName.equalsIgnoreCase("-")) {
                applyCreator(player, buildWorld, null);
                return;
            }

            resolvePlayer(
                    playerLookupService,
                    player,
                    creatorName,
                    "worlds_setcreator_player_not_found",
                    creator -> applyCreator(player, buildWorld, creator));
        });
    }

    private void applyCreator(Player player, BuildWorld buildWorld, @Nullable Builder creator) {
        buildWorld.getBuilders().setCreator(creator);

        settingsService.forceUpdateSidebar(buildWorld);
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        messages.sendMessage(player, "worlds_setcreator_set", Placeholders.of("%world%", buildWorld.getName()));
        player.closeInventory();
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        return completeWorldName(player, args);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.SET_CREATOR;
    }
}
