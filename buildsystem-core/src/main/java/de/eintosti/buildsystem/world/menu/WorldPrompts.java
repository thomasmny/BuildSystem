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
package de.eintosti.buildsystem.world.menu;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The chat prompts for editing a world's project, permission and builders, shared by the {@code /worlds} subcommands
 * and the world editor menus. {@code then} runs when a prompt is finished or cancelled: the commands close the
 * inventory, the menus reopen themselves.
 */
@NullMarked
public final class WorldPrompts {

    private final Messages messages;
    private final Prompts prompts;
    private final SettingsService settingsService;
    private final ConfigService configService;
    private final PlayerLookupService playerLookupService;
    private final TaskScheduler scheduler;

    public WorldPrompts(
            Messages messages,
            Prompts prompts,
            SettingsService settingsService,
            ConfigService configService,
            PlayerLookupService playerLookupService,
            TaskScheduler scheduler) {
        this.messages = messages;
        this.prompts = prompts;
        this.settingsService = settingsService;
        this.configService = configService;
        this.playerLookupService = playerLookupService;
        this.scheduler = scheduler;
    }

    public void promptProject(Player player, BuildWorld buildWorld, Runnable then) {
        prompts.prompt(player).title("enter_world_project").onCancel(then).request(input -> {
            String project = input.trim();
            buildWorld.getData().set(WorldDataKey.PROJECT, project);
            settingsService.forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            messages.sendMessage(player, "worlds_setproject_set", Placeholders.of("%world%", buildWorld.getName()));

            // The world's folder can override this value, in which case the stored project is not the one shown.
            String effective = buildWorld.getData().get(WorldDataKey.PROJECT);
            if (!project.equals(effective)) {
                messages.sendMessage(player, "worlds_setproject_overridden", Placeholders.of("%project%", effective));
            }
            then.run();
        });
    }

    public void promptPermission(Player player, BuildWorld buildWorld, Runnable then) {
        prompts.prompt(player).title("enter_world_permission").onCancel(then).request(input -> {
            String permission = input.trim();

            List<String> whitelist = configService.current().settings().worldPermissionWhitelist();
            if (!isPermissionAllowed(permission, whitelist)) {
                XSound.ENTITY_ITEM_BREAK.play(player);
                messages.sendMessage(player, "worlds_setpermission_not_allowed");
                then.run();
                return;
            }

            buildWorld.getData().set(WorldDataKey.PERMISSION, permission);
            settingsService.forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            messages.sendMessage(player, "worlds_setpermission_set", Placeholders.of("%world%", buildWorld.getName()));

            // A folder override means the stored permission is not the one being enforced.
            String effective = buildWorld.getData().get(WorldDataKey.PERMISSION);
            if (!permission.equals(effective)) {
                messages.sendMessage(
                        player, "worlds_setpermission_overridden", Placeholders.of("%permission%", effective));
            }
            then.run();
        });
    }

    /**
     * Determines whether a permission lock may be set via {@code /worlds setPermission}.
     *
     * <p>An empty whitelist imposes no restriction. When the whitelist is non-empty, only listed values and the
     * {@code "-"} sentinel (which clears the permission) are allowed.
     *
     * @param input The trimmed permission input
     * @param whitelist The configured whitelist of permitted permission strings
     * @return {@code true} if the permission may be set, {@code false} otherwise
     */
    static boolean isPermissionAllowed(String input, List<String> whitelist) {
        return whitelist.isEmpty() || input.equals("-") || whitelist.contains(input);
    }

    public void promptAddBuilder(Player player, BuildWorld buildWorld, Runnable then) {
        if (!buildWorld.getPermissions().canPerformCommand(player, Permissions.ADDBUILDER)) {
            messages.sendPermissionError(player);
            return;
        }

        prompts.prompt(player)
                .title("enter_player_name")
                .onCancel(then)
                .request(input -> addBuilder(player, buildWorld, input.trim(), then));
    }

    /**
     * Adds the named player as a builder. {@code then} runs after a successful add; a refusal closes the inventory.
     */
    public void addBuilder(Player player, BuildWorld buildWorld, String builderName, Runnable then) {
        playerLookupService.resolve(
                builderName, scheduler.mainThread(), builder -> applyBuilder(player, buildWorld, builder, then), () -> {
                    messages.sendMessage(player, "worlds_addbuilder_player_not_found");
                    player.closeInventory();
                });
    }

    private void applyBuilder(Player player, BuildWorld buildWorld, Builder builder, Runnable then) {
        Builders builders = buildWorld.getBuilders();
        if (builder.getUniqueId().equals(player.getUniqueId()) && builders.isCreator(player)) {
            messages.sendMessage(player, "worlds_addbuilder_already_creator");
            player.closeInventory();
            return;
        }

        if (builders.isBuilder(builder.getUniqueId())) {
            messages.sendMessage(player, "worlds_addbuilder_already_added");
            player.closeInventory();
            return;
        }

        builders.addBuilder(builder);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        messages.sendMessage(player, "worlds_addbuilder_added", Placeholders.of("%builder%", builder.getName()));
        then.run();
    }
}
