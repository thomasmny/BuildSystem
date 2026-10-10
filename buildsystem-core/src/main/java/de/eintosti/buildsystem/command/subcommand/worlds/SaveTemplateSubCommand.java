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
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.FeedbackSound;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.WorldFlush;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SaveTemplateSubCommand extends WorldSubCommand {

    private final ConfigService configService;
    private final File dataFolder;
    private final Logger logger;
    private final TaskScheduler scheduler;

    public SaveTemplateSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            ConfigService configService,
            File dataFolder,
            Logger logger,
            TaskScheduler scheduler) {
        super(messages, worldService, WorldTarget.argument(3, "worlds_savetemplate"));
        this.configService = configService;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.scheduler = scheduler;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        // A namespace is left out: the colon is not allowed in a template name, and cannot be in a folder on Windows.
        String templateName = args.length == 3 ? args[2] : WorldNames.path(buildWorld.getName());

        String invalidCharacters = configService.current().world().invalidCharacters();
        if (StringCleaner.firstInvalidChar(templateName, invalidCharacters) != null) {
            messages.sendMessage(player, "worlds_savetemplate_invalid_name");
            return;
        }

        File templatesDir = new File(dataFolder, "templates");
        File templateDir = new File(templatesDir, templateName);
        if (StringCleaner.isPathEscape(templatesDir, templateDir)) {
            messages.sendMessage(player, "worlds_savetemplate_invalid_name");
            return;
        }

        if (templateDir.exists()) {
            messages.sendMessage(player, "worlds_savetemplate_exists", Placeholders.of("%template%", templateName));
            return;
        }

        File worldDir = FileUtils.worldFolder(buildWorld.getName());
        if (!worldDir.exists()) {
            messages.sendMessage(player, "worlds_savetemplate_no_directory");
            return;
        }

        // Held like a backup: without it a delete or rename could pull the folder away mid-copy, and the autosave
        // could rewrite region files the copy is reading.
        WorldOperations operations = worldService.operations();
        operations
                .runExclusively(buildWorld, () -> {
                    Optional<World> world = buildWorld.getWorld();
                    world.ifPresent(WorldFlush::saveAndPauseWrites);
                    messages.sendMessage(
                            player,
                            "worlds_savetemplate_started",
                            Placeholders.of()
                                    .add("%world%", buildWorld.getName())
                                    .add("%template%", templateName)
                                    .build());
                    return CompletableFuture.runAsync(
                                    () -> {
                                        try {
                                            FileUtils.copy(worldDir, templateDir);
                                        } catch (IOException e) {
                                            throw new CompletionException(e);
                                        }
                                    },
                                    scheduler.background())
                            .whenCompleteAsync(
                                    (ignored, throwable) -> world.ifPresent(WorldFlush::resumeWrites),
                                    scheduler.mainThread());
                })
                .whenCompleteAsync(
                        (ignored, throwable) -> {
                            if (throwable == null) {
                                FeedbackSound.SUCCESS.play(player);
                                messages.sendMessage(
                                        player,
                                        "worlds_savetemplate_finished",
                                        Placeholders.of("%template%", templateName));
                            } else if (!operations.reportRefusal(player, buildWorld.getName(), throwable)) {
                                logger.log(
                                        Level.SEVERE,
                                        "Failed to save template '" + templateName + "' from world "
                                                + buildWorld.getName(),
                                        throwable);
                                messages.sendMessage(
                                        player,
                                        "worlds_savetemplate_error",
                                        Placeholders.of("%template%", templateName));
                            }
                        },
                        scheduler.mainThread());
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        return completeWorldName(player, args);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.SAVE_TEMPLATE;
    }
}
