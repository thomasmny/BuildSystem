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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.WorldContext;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldBuilderImpl extends AbstractWorldCreator implements WorldBuilder {

    private static final String TEMPLATES_DIRECTORY = "templates";

    private final File dataFolder;
    private @Nullable String template = null;

    public WorldBuilderImpl(WorldContext context, WorldStorageImpl worldStorage, File dataFolder, String worldName) {
        super(context, worldStorage, worldName, BuildWorldType.NORMAL);
        this.dataFolder = dataFolder;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl type(BuildWorldType type) {
        this.worldType = type;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl template(@Nullable String template) {
        this.template = ChatColor.stripColor(template);
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl customGenerator(@Nullable CustomGenerator customGenerator) {
        this.customGenerator = customGenerator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl difficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl time(int time) {
        this.time = time;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl worldBorderSize(int worldBorderSize) {
        this.worldBorderSize = worldBorderSize;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl seed(long seed) {
        this.seed = seed;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl creator(@Nullable Builder creator) {
        this.creator = creator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl folder(@Nullable Folder folder) {
        this.folder = folder;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl privateWorld(boolean privateWorld) {
        this.privateWorld = privateWorld;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl creationDate(long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldBuilderImpl notify(@Nullable Player audience) {
        this.audience = audience;
        return this;
    }

    @Override
    protected boolean isImport() {
        return false;
    }

    @Override
    public @Nullable BuildWorld build() {
        if (!mayCreate()) {
            return null;
        }

        if (worldType != BuildWorldType.TEMPLATE) {
            return announceCreated(createWorldFromGenerator());
        }

        File templateFile = startTemplateCopy();
        if (templateFile == null) {
            return null;
        }

        File worldFile = FileUtils.worldFolder(worldName);
        try {
            FileUtils.copy(templateFile, worldFile);
        } catch (IOException e) {
            failTemplateCopy(e);
            return null;
        }
        return announceCreated(generateAndRegister(true));
    }

    /**
     * Builds the world like {@link #build()}, but copies a template in the background, so a large template does not
     * freeze the server. Call it on the main thread; the future completes there.
     *
     * @return The created world, or {@code null} when it could not be created
     */
    public CompletableFuture<@Nullable BuildWorld> buildAsync() {
        if (worldType != BuildWorldType.TEMPLATE) {
            return CompletableFuture.completedFuture(build());
        }

        if (!mayCreate()) {
            return CompletableFuture.completedFuture(null);
        }

        File templateFile = startTemplateCopy();
        if (templateFile == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Created here rather than by the copy, so the name is taken before this method returns: isNameTaken looks
        // at the folder, and the world is only registered once the copy is done. The import lists the folder as soon as
        // the copy has written world data into it, so the name stays reserved until the world is registered or its
        // folder is deleted again.
        File worldFile = FileUtils.worldFolder(worldName);
        worldStorage.reserve(worldName);
        worldFile.mkdirs();
        return CompletableFuture.runAsync(
                        () -> {
                            try {
                                FileUtils.copy(templateFile, worldFile);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        },
                        context.scheduler().background())
                .handleAsync(
                        (ignored, throwable) -> {
                            if (throwable != null) {
                                failTemplateCopy(throwable);
                                return null;
                            }
                            return announceCreated(generateAndRegister(true));
                        },
                        context.scheduler().mainThread())
                .whenComplete((created, throwable) -> {
                    // Thrown by the world generation; a synchronous build() would have surfaced it to the caller.
                    if (throwable != null) {
                        context.logger().log(Level.SEVERE, "Failed to create world " + worldName, throwable);
                        notifyAudience("worlds_creation_failed", Placeholders.of("%world%", worldName));
                    }
                    folderDeleted.whenComplete((ignored, deleteFailure) -> worldStorage.release(worldName));
                });
    }

    private boolean mayCreate() {
        if (worldStorage.isNameTaken(worldName)) {
            notifyAudience("worlds_world_exists");
            return false;
        }
        return !isCreationCancelled();
    }

    private @Nullable BuildWorld announceCreated(@Nullable BuildWorld created) {
        if (created != null) {
            notifyAudience("worlds_creation_finished");
        }
        return created;
    }

    private @Nullable BuildWorld createWorldFromGenerator() {
        if (audience != null) {
            notifyAudience(
                    "worlds_world_creation_started",
                    Placeholders.of()
                            .add("%world%", worldName)
                            .add("%type%", context.messages().getString(Messages.getMessageKey(worldType), audience))
                            .build());
        }
        return generateAndRegister(false);
    }

    /**
     * Checks the template exists and tells the audience the copy starts.
     *
     * @return The template folder to copy, or {@code null} when there is none
     */
    private @Nullable File startTemplateCopy() {
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("Attempted to create a template world without a template name");
        }

        File templatesDir = new File(dataFolder, TEMPLATES_DIRECTORY);
        File templateFile = new File(dataFolder, TEMPLATES_DIRECTORY + File.separator + template);
        if (StringCleaner.isPathEscape(templatesDir, templateFile)) {
            notifyAudience("worlds_template_does_not_exist");
            return null;
        }

        if (!templateFile.exists()) {
            notifyAudience("worlds_template_does_not_exist");
            return null;
        }

        notifyAudience(
                "worlds_template_creation_started",
                Placeholders.of()
                        .add("%world%", worldName)
                        .add("%template%", template)
                        .build());
        return templateFile;
    }

    /**
     * Reports a failed template copy and deletes what it left behind. Without this the world was registered around a
     * half-copied (or empty) directory and reported as created.
     */
    private void failTemplateCopy(Throwable failure) {
        context.logger()
                .log(Level.SEVERE, "Failed to copy template \"" + template + "\" for world " + worldName, failure);
        notifyAudience("worlds_template_creation_error", Placeholders.of("%template%", template));
        deleteFolderOfFailedWorld();
    }
}
