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
import de.eintosti.buildsystem.api.world.creation.BuildWorldCreator;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.i18n.Messages;
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

@NullMarked
public class BuildWorldCreatorImpl implements BuildWorldCreator {

    private static final String TEMPLATES_DIRECTORY = "templates";

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;
    private final WorldDataVersionGuard versionGuard;

    private String worldName;
    @Nullable
    private Builder creator;
    private boolean isPrivate = false;
    private BuildWorldType worldType = BuildWorldType.NORMAL;
    @Nullable
    private CustomGenerator customGenerator = null;
    private long creationDate = System.currentTimeMillis();
    @Nullable
    private String template = null;
    @Nullable
    private Folder folder;

    @Nullable
    private Difficulty difficulty;
    @Nullable
    private Integer time;
    @Nullable
    private Integer worldBorderSize;

    @Nullable
    private BuildWorld buildWorld;

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, String name) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        this.worldName = name;
        this.difficulty = plugin.getConfigService().current().world().defaults().difficulty();
        this.time = plugin.getConfigService().current().world().defaults().time().noon();
        this.worldBorderSize = plugin.getConfigService().current().world().defaults().worldBorderSize();
        this.versionGuard = new WorldDataVersionGuard(plugin.getLogger(), name);
    }

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, BuildWorld buildWorld) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        this.buildWorld = buildWorld;
        this.worldName = buildWorld.getName();
        this.worldType = buildWorld.getType();
        this.customGenerator = buildWorld.getCustomGenerator();
        this.versionGuard = new WorldDataVersionGuard(plugin.getLogger(), buildWorld.getName());
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setName(String name) {
        this.worldName = name;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setCreator(@Nullable Builder creator) {
        this.creator = creator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setTemplate(@Nullable String template) {
        this.template = ChatColor.stripColor(template);
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setType(BuildWorldType type) {
        this.worldType = type;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setCustomGenerator(@Nullable CustomGenerator customGenerator) {
        this.customGenerator = customGenerator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setFolder(@Nullable Folder folder) {
        this.folder = folder;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setCreationDate(long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Override
    public void createWorld(Player player) {
        if (worldStorage.worldAndFolderExist(worldName)) {
            plugin.getMessages().sendMessage(player, "worlds_world_exists");
            return;
        }

        boolean success = (worldType == BuildWorldType.TEMPLATE)
                ? createWorldFromTemplate(player)
                : createWorldFromGenerator(player);

        if (success) {
            teleportAfterCreation(player);
            plugin.getMessages().sendMessage(player, "worlds_creation_finished");
        }
    }

    @Override
    public void importWorld(Player player, boolean teleport) {
        buildWorld = createAndRegisterBuildWorld(player);
        generateBukkitWorld(true);
        if (teleport) {
            teleportAfterCreation(player);
        }
    }

    private boolean createWorldFromGenerator(Player player) {
        plugin.getMessages().sendMessage(player, "worlds_world_creation_started",
                Map.entry("%world%", worldName),
                Map.entry("%type%", plugin.getMessages().getString(Messages.getMessageKey(worldType), player))
        );
        buildWorld = createAndRegisterBuildWorld(player);
        generateBukkitWorld(false);
        return true;
    }

    private boolean createWorldFromTemplate(Player player) {
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("Attempted to create a template world without a template name");
        }

        File templateFile = new File(plugin.getDataFolder(), TEMPLATES_DIRECTORY + File.separator + template);
        if (!templateFile.exists()) {
            plugin.getMessages().sendMessage(player, "worlds_template_does_not_exist");
            return false;
        }

        plugin.getMessages().sendMessage(player, "worlds_template_creation_started",
                Map.entry("%world%", worldName),
                Map.entry("%template%", template)
        );

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        FileUtils.copy(templateFile, worldFile);

        buildWorld = createAndRegisterBuildWorld(player);
        generateBukkitWorld(true);
        return true;
    }

    private BuildWorld createAndRegisterBuildWorld(Player player) {
        BuildWorldImpl bw = new BuildWorldImpl(
                worldName,
                creator == null ? Builder.of(player) : creator,
                worldType,
                creationDate,
                isPrivate,
                customGenerator,
                folder
        );

        if (folder != null) {
            folder.addWorld(bw);
        }

        bw.getData().lastLoaded().set(System.currentTimeMillis());
        worldStorage.addBuildWorld(bw);
        return bw;
    }

    @Override
    @Nullable
    public World generateBukkitWorld(boolean checkVersion) {
        if (buildWorld == null) {
            throw new IllegalStateException("BuildWorld must be set before generating the Bukkit world.");
        }
        return new BukkitWorldFactory(plugin, worldName, worldType, customGenerator, difficulty, time, worldBorderSize)
                .generate(checkVersion ? BukkitWorldFactory.VersionCheck.REQUIRED : BukkitWorldFactory.VersionCheck.SKIP);
    }

    public boolean isDataVersionTooHigh() {
        return versionGuard.isDataVersionTooHigh();
    }

    private void teleportAfterCreation(Player player) {
        BuildWorld bw = worldStorage.getBuildWorld(worldName);
        if (bw == null) {
            return;
        }
        bw.getUnloader().manageUnload();
        bw.getTeleporter().teleport(player);
    }
}
