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
import java.util.Objects;
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
        Prompts promptsInstance = prompts.get();
        Selection selection = new Selection();

        Prompts.PromptFlow flow = promptsInstance
                .flow(player)
                .step("enter_world_name", input -> nameStep(promptsInstance, player, selection, input));

        if (worldType == BuildWorldType.CUSTOM) {
            flow.step("enter_generator_name", input -> generatorStep(player, selection, input));
        } else if (promptSeed) {
            flow.step("enter_world_seed", input -> seedStep(selection, input));
        }

        flow.start(() -> build(player, selection, worldType, template, privateWorld, folder));
    }

    private boolean nameStep(Prompts promptsInstance, Player player, Selection selection, String input) {
        String name = promptsInstance.sanitizeName(
                player, input, "worlds_world_creation_invalid_characters", "worlds_world_creation_name_bank");
        if (name == null) {
            return false;
        }

        if (worldService.getWorldStorage().worldAndFolderExist(name)) {
            messages.sendMessage(player, "worlds_world_exists");
            XSound.ENTITY_ITEM_BREAK.play(player);
            return false;
        }

        selection.name = name;
        return true;
    }

    private boolean seedStep(Selection selection, String input) {
        // Vanilla seed semantics: numeric input is the literal seed, other text hashes to a long, and a blank entry
        // falls through to Bukkit's random seed.
        String trimmed = input.trim();
        if (!trimmed.isEmpty()) {
            selection.seed = parseSeed(trimmed);
        }
        return true;
    }

    private boolean generatorStep(Player player, Selection selection, String input) {
        // Generator names are dynamic and cannot be pre-registered in plugin.yml, so default-allow is emulated: a
        // generator is permitted unless an admin has explicitly denied its specific node.
        String generatorNode = "buildsystem.create.generator." + input.trim();
        boolean allowed = !player.isPermissionSet(generatorNode) || player.hasPermission(generatorNode);
        if (!allowed) {
            messages.sendPermissionError(player);
            XSound.ENTITY_ITEM_BREAK.play(player);
            return false;
        }

        CustomGenerator generator = CustomGeneratorImpl.of(input, selection.name());
        if (generator == null) {
            messages.sendMessage(player, "worlds_import_unknown_generator");
            XSound.ENTITY_ITEM_BREAK.play(player);
            return false;
        }

        selection.generator = generator;
        return true;
    }

    private void build(
            Player player,
            Selection selection,
            BuildWorldType worldType,
            @Nullable String template,
            boolean privateWorld,
            @Nullable Folder folder) {
        WorldBuilder builder = worldService
                .newWorld(selection.name())
                .type(worldType)
                .template(template)
                .privateWorld(privateWorld)
                .folder(folder)
                .customGenerator(selection.generator);
        Long seed = selection.seed;
        if (seed != null) {
            builder.seed(seed);
        }
        buildAndTeleport(player, builder);
    }

    private static long parseSeed(String seed) {
        try {
            return Long.parseLong(seed);
        } catch (NumberFormatException e) {
            return seed.hashCode();
        }
    }

    private void buildAndTeleport(Player player, WorldBuilder worldBuilder) {
        BuildWorld world =
                worldBuilder.creator(Builder.of(player)).notify(player).build();
        if (world != null) {
            world.getTeleporter().teleport(player);
        }
    }

    /**
     * The values collected across the creation steps; the builder is assembled from them once all steps accept.
     */
    private static final class Selection {
        private @Nullable String name;
        private @Nullable Long seed;
        private @Nullable CustomGenerator generator;

        /**
         * {@return the name validated by the name step} The flow only reaches later steps once it is set.
         */
        private String name() {
            return Objects.requireNonNull(name, "name step must accept before later steps run");
        }
    }
}
