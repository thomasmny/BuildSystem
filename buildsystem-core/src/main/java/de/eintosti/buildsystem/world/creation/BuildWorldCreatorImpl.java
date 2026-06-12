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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import de.eintosti.buildsystem.api.world.creation.WorldImporter;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import java.io.File;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Shared implementation backing both {@link WorldBuilder} (generating a fresh world) and {@link WorldImporter}
 * (adopting an existing directory). The active mode is fixed at construction; the consumer only ever sees one of the
 * two interfaces, so the generate-only and import-only setters cannot be mixed.
 */
@NullMarked
public class BuildWorldCreatorImpl implements WorldBuilder, WorldImporter {

    private static final String TEMPLATES_DIRECTORY = "templates";

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;
    private final WorldDataVersionGuard versionGuard;
    private final boolean importMode;

    private final String worldName;
    private @Nullable Builder creator;
    private boolean isPrivate = false;
    private BuildWorldType worldType;
    private @Nullable CustomGenerator customGenerator = null;
    private long creationDate = System.currentTimeMillis();

    private @Nullable String template = null;
    private @Nullable Folder folder;

    private @Nullable Difficulty difficulty;
    private @Nullable Integer time;
    private @Nullable Integer worldBorderSize;

    private @Nullable Player audience;

    private @Nullable BuildWorld buildWorld;

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, String name) {
        this(plugin, name, false);
    }

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, String name, boolean importMode) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.importMode = importMode;

        this.worldName = name;
        this.worldType = importMode ? BuildWorldType.IMPORTED : BuildWorldType.NORMAL;
        this.difficulty = plugin.getConfigService().current().world().defaults().difficulty();
        this.time =
                plugin.getConfigService().current().world().defaults().time().noon();
        this.worldBorderSize =
                plugin.getConfigService().current().world().defaults().worldBorderSize();
        this.versionGuard = new WorldDataVersionGuard(plugin.getLogger(), name);
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl creator(@Nullable Builder creator) {
        this.creator = creator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl template(@Nullable String template) {
        this.template = ChatColor.stripColor(template);
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl type(BuildWorldType type) {
        this.worldType = type;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl customGenerator(@Nullable CustomGenerator customGenerator) {
        this.customGenerator = customGenerator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl folder(@Nullable Folder folder) {
        this.folder = folder;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl privateWorld(boolean privateWorld) {
        this.isPrivate = privateWorld;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl difficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl creationDate(long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl notify(@Nullable Player audience) {
        this.audience = audience;
        return this;
    }

    @Override
    @Nullable public BuildWorld build() {
        return importMode ? buildImported() : buildGenerated();
    }

    private @Nullable BuildWorld buildGenerated() {
        if (worldStorage.worldAndFolderExist(worldName)) {
            notifyAudience("worlds_world_exists");
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

    private @Nullable BuildWorld buildImported() {
        if (isDataVersionTooHigh()) {
            notifyAudience("worlds_import_newer_version", Map.entry("%world%", worldName));
            return null;
        }

        if (customGenerator == null) {
            // Imported worlds default to a void generator so unexplored chunks are not filled with terrain.
            customGenerator = new CustomGeneratorImpl("BuildSystem", "void", null);
        }

        buildWorld = createAndRegisterBuildWorld();
        generateBukkitWorld(true);
        return buildWorld;
    }

    private boolean createWorldFromGenerator() {
        if (audience != null) {
            notifyAudience(
                    "worlds_world_creation_started",
                    Map.entry("%world%", worldName),
                    Map.entry("%type%", plugin.getMessages().getString(Messages.getMessageKey(worldType), audience)));
        }
        buildWorld = createAndRegisterBuildWorld();
        generateBukkitWorld(false);
        return true;
    }

    private boolean createWorldFromTemplate() {
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("Attempted to create a template world without a template name");
        }

        File templatesDir = new File(plugin.getDataFolder(), TEMPLATES_DIRECTORY);
        File templateFile = new File(plugin.getDataFolder(), TEMPLATES_DIRECTORY + File.separator + template);
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

    private BuildWorld createAndRegisterBuildWorld() {
        BuildWorldImpl bw = new BuildWorldImpl(
                plugin, worldName, creator, worldType, creationDate, isPrivate, customGenerator, folder);

        if (folder != null) {
            folder.addWorld(bw);
        }

        bw.getData().lastLoaded().set(System.currentTimeMillis());
        worldStorage.addBuildWorld(bw);
        return bw;
    }

    public @Nullable World generateBukkitWorld(boolean checkVersion) {
        if (buildWorld == null) {
            throw new IllegalStateException("BuildWorld must be set before generating the Bukkit world.");
        }

        return new BukkitWorldFactory(plugin, worldName, worldType, customGenerator, difficulty, time, worldBorderSize)
                .generate(
                        checkVersion ? BukkitWorldFactory.VersionCheck.REQUIRED : BukkitWorldFactory.VersionCheck.SKIP);
    }

    public boolean isDataVersionTooHigh() {
        return versionGuard.isDataVersionTooHigh();
    }

    @SafeVarargs
    private void notifyAudience(String key, Map.Entry<String, Object>... placeholders) {
        if (audience != null) {
            plugin.getMessages().sendMessage(audience, key, placeholders);
        }
    }
}
