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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Drives the chat-input flow for creating a new world: prompting for a name, optionally a custom generator, then
 * building and teleporting the player.
 */
@NullMarked
public class WorldCreationPrompts {

    private final WorldServiceImpl worldService;
    private final Supplier<Prompts> prompts;
    private final Messages messages;

    public WorldCreationPrompts(WorldServiceImpl worldService, Supplier<Prompts> prompts, Messages messages) {
        this.worldService = worldService;
        this.prompts = prompts;
        this.messages = messages;
    }

    public void startWorldNameInput(
            Player player,
            BuildWorldType worldType,
            @Nullable String template,
            boolean privateWorld,
            boolean promptSeed,
            @Nullable Folder folder) {
        player.closeInventory();
        prompts.get()
                .prompt(player)
                .title("enter_world_name")
                .sanitizeName("worlds_world_creation_invalid_characters", "worlds_world_creation_name_bank")
                .request(worldName -> {
                    if (worldType == BuildWorldType.CUSTOM) {
                        startCustomGeneratorInput(player, worldName, template, privateWorld, folder);
                    } else if (promptSeed) {
                        startSeedInput(player, worldName, worldType, template, privateWorld, folder);
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

    private void startSeedInput(
            Player player,
            String worldName,
            BuildWorldType worldType,
            @Nullable String template,
            boolean privateWorld,
            @Nullable Folder folder) {
        prompts.get().prompt(player).title("enter_world_seed").request(input -> {
            WorldBuilder worldBuilder = worldService
                    .newWorld(worldName)
                    .type(worldType)
                    .template(template)
                    .privateWorld(privateWorld)
                    .folder(folder);

            // Vanilla seed semantics: numeric input is the literal seed, other text hashes to a long, and a blank entry
            // falls through to Bukkit's random seed.
            String trimmed = input.trim();
            if (!trimmed.isEmpty()) {
                worldBuilder.seed(parseSeed(trimmed));
            }
            buildAndTeleport(player, worldBuilder);
        });
    }

    private static long parseSeed(String seed) {
        try {
            return Long.parseLong(seed);
        } catch (NumberFormatException e) {
            return seed.hashCode();
        }
    }

    private void startCustomGeneratorInput(
            Player player, String worldName, @Nullable String template, boolean privateWorld, @Nullable Folder folder) {
        prompts.get().prompt(player).title("enter_generator_name").request(input -> {
            // Generator names are dynamic and cannot be pre-registered in plugin.yml, so default-allow is emulated: a
            // generator is permitted unless an admin has explicitly denied its specific node.
            String generatorNode = "buildsystem.create.generator." + input.trim();
            boolean allowed = !player.isPermissionSet(generatorNode) || player.hasPermission(generatorNode);
            if (!allowed) {
                messages.sendPermissionError(player);
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            }

            CustomGenerator customGenerator = CustomGeneratorImpl.of(input, worldName);
            if (customGenerator == null) {
                messages.sendMessage(player, "worlds_import_unknown_generator");
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
