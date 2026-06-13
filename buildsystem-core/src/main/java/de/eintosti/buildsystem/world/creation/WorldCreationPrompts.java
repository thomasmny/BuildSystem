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
package de.eintosti.buildsystem.world.creation;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Drives the chat-input world creation flow: prompting the player for a world name, optionally a custom generator, and
 * then building and teleporting them into the new world.
 */
@NullMarked
public class WorldCreationPrompts {

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;

    public WorldCreationPrompts(BuildSystemPlugin plugin, WorldServiceImpl worldService) {
        this.plugin = plugin;
        this.worldService = worldService;
    }

    public void startWorldNameInput(
            Player player,
            BuildWorldType worldType,
            @Nullable String template,
            boolean privateWorld,
            @Nullable Folder folder) {
        player.closeInventory();
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "enter_world_name",
                "worlds_world_creation_invalid_characters",
                "worlds_world_creation_name_bank",
                worldName -> {
                    if (worldType == BuildWorldType.CUSTOM) {
                        startCustomGeneratorInput(player, worldName, template, privateWorld, folder);
                    } else {
                        buildAndTeleport(
                                player,
                                worldService
                                        .newWorld(worldName)
                                        .type(worldType)
                                        .template(template)
                                        .privateWorld(privateWorld)
                                        .folder(folder));
                    }
                });
    }

    private void startCustomGeneratorInput(
            Player player, String worldName, @Nullable String template, boolean privateWorld, @Nullable Folder folder) {
        new PlayerChatInput(plugin, player, "enter_generator_name", input -> {
            boolean restrictTemplateAccess =
                    plugin.getConfigService().current().settings().restrictTemplateAccess();
            if (restrictTemplateAccess && !player.hasPermission("buildsystem.create.generator." + input.trim())) {
                plugin.getMessages().sendPermissionError(player);
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            }

            CustomGenerator customGenerator = CustomGeneratorImpl.of(input, worldName);
            if (customGenerator == null) {
                plugin.getMessages().sendMessage(player, "worlds_import_unknown_generator");
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            }

            buildAndTeleport(
                    player,
                    worldService
                            .newWorld(worldName)
                            .type(BuildWorldType.CUSTOM)
                            .template(template)
                            .privateWorld(privateWorld)
                            .customGenerator(customGenerator)
                            .folder(folder));
        });
    }

    private void buildAndTeleport(Player player, WorldBuilder worldBuilder) {
        BuildWorld world =
                worldBuilder.creator(Builder.of(player)).notify(player).build();
        if (world != null) {
            world.getTeleporter().teleport(player);
        }
    }
}
