/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.BuildWorldCreator;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.Config.World.Default;
import de.eintosti.buildsystem.config.Config.World.Default.Time;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.creation.generator.voidgenerator.ModernVoidGenerator;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.io.CompressionType;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildWorldCreatorImpl implements BuildWorldCreator {

    private static final String LEVEL_DAT_FILE_NAME = "level.dat";
    private static final String TEMPLATES_DIRECTORY = "templates";
    private static final int VOID_BLOCK_Y = 64;

    private final BuildSystemPlugin plugin;
    private final WorldStorage worldStorage;

    private String worldName;
    private Builder creator;
    private boolean isPrivate = false;
    private BuildWorldType worldType = BuildWorldType.NORMAL;
    private CustomGenerator customGenerator = null;
    private long creationDate = System.currentTimeMillis();
    private String template = null;
    private Difficulty difficulty;
    private Folder folder;

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, @NotNull String name) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        setName(name);
        setDifficulty(Default.difficulty);
    }

    @Override
    public BuildWorldCreatorImpl setName(String name) {
        this.worldName = name;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setCreator(Builder creator) {
        this.creator = creator;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setTemplate(String template) {
        this.template = ChatColor.stripColor(template);
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setType(BuildWorldType type) {
        this.worldType = type;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setCustomGenerator(CustomGenerator customGenerator) {
        this.customGenerator = customGenerator;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setFolder(@Nullable Folder folder) {
        this.folder = folder;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    @Override
    public BuildWorldCreatorImpl setCreationDate(long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Override
    public void createWorld(Player player) {
        if (worldStorage.worldAndFolderExist(worldName)) {
            Messages.sendMessage(player, "worlds_world_exists");
            return;
        }

        boolean success = (worldType == BuildWorldType.TEMPLATE)
                ? createWorldFromTemplate(player)
                : createWorldFromGenerator(player);

        if (success) {
            teleportAfterCreation(player);
            Messages.sendMessage(player, "worlds_creation_finished");
        }
    }

    @Override
    public void importWorld(Player player, boolean teleport) {
        BuildWorld buildWorld = createAndRegisterBuildWorld(player);
        generateBukkitWorld(buildWorld, true);
        if (teleport) {
            teleportAfterCreation(player);
        }
    }

    /**
     * Handles the creation of worlds that use a generator (i.e., not a template).
     *
     * @param player The player creating the world
     * @return true if the process started successfully, false otherwise
     */
    private boolean createWorldFromGenerator(Player player) {
        Messages.sendMessage(player, "worlds_world_creation_started",
                Map.entry("%world%", worldName),
                Map.entry("%type%", Messages.getString(Messages.getMessageKey(worldType), player))
        );

        BuildWorld buildWorld = createAndRegisterBuildWorld(player);
        generateBukkitWorld(buildWorld, false); // Version check is not needed for new worlds.
        return true;
    }

    /**
     * Handles the creation of worlds from a template file.
     *
     * @param player The player creating the world
     * @return {@code true} if the creation was successful, {@code false} otherwise
     */
    private boolean createWorldFromTemplate(Player player) {
        File templateFile = new File(plugin.getDataFolder(), TEMPLATES_DIRECTORY + File.separator + template);
        if (!templateFile.exists()) {
            Messages.sendMessage(player, "worlds_template_does_not_exist");
            return false;
        }

        Messages.sendMessage(player, "worlds_template_creation_started",
                Map.entry("%world%", worldName),
                Map.entry("%template%", template)
        );

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        FileUtils.copy(templateFile, worldFile);

        BuildWorld buildWorld = createAndRegisterBuildWorld(player);
        generateBukkitWorld(buildWorld, true); // Version check is important for templates.
        return true;
    }

    /**
     * Creates the {@link BuildWorld} object and registers it.
     *
     * @param player The player creating the world
     * @return The newly created {@link BuildWorld} instance
     */
    private BuildWorld createAndRegisterBuildWorld(Player player) {
        BuildWorldImpl buildWorld = new BuildWorldImpl(
                worldName,
                creator == null ? Builder.of(player) : creator,
                worldType,
                creationDate,
                isPrivate,
                customGenerator
        );

        buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        worldStorage.addBuildWorld(buildWorld);
        if (folder != null) {
            folder.addWorld(buildWorld);
        }

        return buildWorld;
    }

    /**
     * Retrieves the server's data version.
     *
     * @return The server's data version
     */
    @SuppressWarnings("deprecation")
    private int getServerDataVersion() {
        return Bukkit.getServer().getUnsafe().getDataVersion();
    }

    @Override
    @Nullable
    public World generateBukkitWorld(BuildWorld buildWorld, boolean checkVersion) {
        if (checkVersion && isDataVersionTooHigh()) {
            plugin.getLogger().warning(String.format(Locale.ROOT,
                    "\"%s\" was created in a newer version of Minecraft (%s > %s). Skipping...",
                    worldName, parseDataVersion(), getServerDataVersion()
            ));
            return null;
        }

        WorldCreator worldCreator = createBukkitWorldCreator();
        World bukkitWorld = Bukkit.createWorld(worldCreator);

        if (bukkitWorld != null) {
            applyDefaultWorldSettings(bukkitWorld);
            applyPostGenerationSettings(bukkitWorld, buildWorld.getType());
            updateWorldDataVersion();
        }

        return bukkitWorld;
    }

    /**
     * Creates and configures a {@link WorldCreator} based on the specified {@link BuildWorldType}.
     *
     * @return A configured {@link WorldCreator}
     */
    private WorldCreator createBukkitWorldCreator() {
        WorldCreator worldCreator = new WorldCreator(worldName);

        switch (worldType) {
            case VOID:
                configureVoidWorld(worldCreator);
                break;
            case FLAT:
            case PRIVATE:
                worldCreator.generateStructures(false);
                worldCreator.type(org.bukkit.WorldType.FLAT);
                break;
            case NETHER:
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.NETHER);
                break;
            case END:
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.THE_END);
                break;
            case CUSTOM:
                if (customGenerator != null) {
                    worldCreator.generator(customGenerator.chunkGenerator());
                }
                // Fall-through to NORMAL for default settings
            default: // NORMAL
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.NORMAL);
                break;
        }
        return worldCreator;
    }

    private void configureVoidWorld(WorldCreator worldCreator) {
        worldCreator.generateStructures(false);
        worldCreator.type(org.bukkit.WorldType.FLAT);
        worldCreator.generator(new ModernVoidGenerator());
    }

    /**
     * Applies standard server settings (difficulty, time, border, gamerules) to a newly created world.
     *
     * @param bukkitWorld The world to configure
     */
    private void applyDefaultWorldSettings(World bukkitWorld) {
        bukkitWorld.setDifficulty(this.difficulty);
        bukkitWorld.setTime(Time.noon);
        bukkitWorld.getWorldBorder().setSize(Default.worldBoarderSize);
        bukkitWorld.setKeepSpawnInMemory(true);
        Default.gameRules.forEach((gameRule, value) -> applyGameRule(bukkitWorld, gameRule, value));
    }

    private <T> void applyGameRule(World world, GameRule<T> rule, Object value) {
        @SuppressWarnings("unchecked")
        T castedValue = (T) value;
        world.setGameRule(rule, castedValue);
    }

    /**
     * Applies settings that are specific to the world type after it has been generated.
     *
     * @param bukkitWorld The world to modify
     * @param worldType   The type of the world
     */
    private void applyPostGenerationSettings(World bukkitWorld, BuildWorldType worldType) {
        switch (worldType) {
            case VOID -> {
                bukkitWorld.getBlockAt(0, VOID_BLOCK_Y, 0).setType(Material.GOLD_BLOCK);
                bukkitWorld.setSpawnLocation(0, VOID_BLOCK_Y + 1, 0);
            }
            case FLAT -> {
                bukkitWorld.setSpawnLocation(0, -60, 0);
            }
            default -> {
                // No special post-generation steps for other types
            }
        }
    }

    public boolean isDataVersionTooHigh() {
        if (Boolean.getBoolean("Paper.ignoreWorldDataVersion")) {
            return false;
        }
        int worldVersion = parseDataVersion();
        return worldVersion > getServerDataVersion();
    }

    private int parseDataVersion() {
        File levelFile = new File(new File(Bukkit.getWorldContainer(), worldName), LEVEL_DAT_FILE_NAME);
        if (!levelFile.exists()) {
            return -1;
        }

        try {
            CompoundTag level = new Nbt().fromFile(levelFile);
            CompoundTag data = level.get("Data");
            IntTag dataVersion = data.getInt("DataVersion");
            return dataVersion != null ? dataVersion.getValue() : -1;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse level.dat for world " + worldName, e);
            return -1;
        }
    }

    private void updateWorldDataVersion() {
        File levelFile = new File(new File(Bukkit.getWorldContainer(), worldName), LEVEL_DAT_FILE_NAME);
        if (!levelFile.exists()) {
            return;
        }

        try {
            Nbt nbt = new Nbt();
            CompoundTag level = nbt.fromFile(levelFile);
            CompoundTag data = level.get("Data");
            IntTag dataVersionTag = data.getInt("DataVersion");
            if (dataVersionTag == null) {
                return;
            }

            int worldVersion = dataVersionTag.getValue();
            int serverVersion = getServerDataVersion();
            if (worldVersion < serverVersion) {
                dataVersionTag.setValue(serverVersion);
                nbt.toFile(level, levelFile, CompressionType.GZIP);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update level.dat for world " + worldName, e);
        }
    }

    private void teleportAfterCreation(Player player) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld == null) {
            return;
        }

        buildWorld.getUnloader().manageUnload();
        buildWorld.getTeleporter().teleport(player);
    }
}