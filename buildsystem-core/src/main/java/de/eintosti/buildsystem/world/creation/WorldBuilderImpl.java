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
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.WorldContext;
import java.io.File;
import java.util.Map;
import org.bukkit.Bukkit;
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
        if (worldStorage.worldAndFolderExist(worldName)) {
            notifyAudience("worlds_world_exists");
            return null;
        }

        if (isCreationCancelled()) {
            return null;
        }

        boolean success =
                (worldType == BuildWorldType.TEMPLATE) ? createWorldFromTemplate() : createWorldFromGenerator();
        if (!success) {
            return null;
        }

        notifyAudience("worlds_creation_finished");
        return buildWorld;
    }

    private boolean createWorldFromGenerator() {
        if (audience != null) {
            notifyAudience(
                    "worlds_world_creation_started",
                    Map.entry("%world%", worldName),
                    Map.entry("%type%", context.messages().getString(Messages.getMessageKey(worldType), audience)));
        }
        buildWorld = createAndRegisterBuildWorld();
        generateBukkitWorld(false);
        return true;
    }

    private boolean createWorldFromTemplate() {
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("Attempted to create a template world without a template name");
        }

        File templatesDir = new File(dataFolder, TEMPLATES_DIRECTORY);
        File templateFile = new File(dataFolder, TEMPLATES_DIRECTORY + File.separator + template);
        if (StringCleaner.isPathEscape(templatesDir, templateFile)) {
            notifyAudience("worlds_template_does_not_exist");
            return false;
        }

        if (!templateFile.exists()) {
            notifyAudience("worlds_template_does_not_exist");
            return false;
        }

        notifyAudience(
                "worlds_template_creation_started", Map.entry("%world%", worldName), Map.entry("%template%", template));

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        FileUtils.copy(templateFile, worldFile);

        buildWorld = createAndRegisterBuildWorld();
        generateBukkitWorld(true);
        return true;
    }
}
