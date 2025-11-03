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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.BuildWorldCreator;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.Config.World.Default;
import de.eintosti.buildsystem.config.Config.World.Default.Time;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl.WorldGenerationData.CustomGeneratorData;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl.WorldGenerationData.PredefinedGeneratorData;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.creation.generator.VoidGenerator;
import de.eintosti.buildsystem.world.modification.GameRuleEntry;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.io.CompressionType;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BuildWorldCreatorImpl implements BuildWorldCreator {

    private static final String LEVEL_DAT_FILE_NAME = "level.dat";
    private static final String WORLD_TYPE_FILE_NAME = ".buildsystem-generator-data.txt";
    private static final String CUSTOM_GENERATOR_PREFIX = "GENERATOR:";

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;

    private String worldName;
    private @Nullable Builder creator;
    private boolean isPrivate = false;
    private BuildWorldType worldType = BuildWorldType.NORMAL;
    private @Nullable CustomGenerator customGenerator = null;
    private long creationDate = System.currentTimeMillis();
    private @Nullable File reference = null;
    private @Nullable Folder folder;

    private @Nullable Difficulty difficulty;
    private @Nullable Integer time;
    private @Nullable Integer worldBoarderSize;

    /**
     * The {@link BuildWorld} associated with this creator.
     * <p>
     * This field is set by the second constructor (for existing worlds) OR by {@link #createWorld(Player)} and {@link #importWorld(Player, boolean)}. It is required for
     * {@link #generateBukkitWorld(boolean)} to function, allowing worlds to be loaded by the plugin after a restart.
     */
    private @Nullable BuildWorld buildWorld;

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, String name) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        this.worldName = name;
        this.difficulty = Default.difficulty;
        this.time = Time.noon;
        this.worldBoarderSize = Default.worldBoarderSize;
    }

    public BuildWorldCreatorImpl(BuildSystemPlugin plugin, BuildWorld buildWorld) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        this.buildWorld = buildWorld;
        this.worldName = buildWorld.getName();
        this.worldType = buildWorld.getType();
        this.customGenerator = buildWorld.getCustomGenerator();
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

    @SuppressWarnings("removal")
    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setTemplate(@Nullable String template) {
        if (template == null) {
            this.reference = null;
        } else {
            this.reference = new File(this.plugin.getDataFolder(), "templates" + File.separator + ChatColor.stripColor(template));
        }
        return this;
    }

    @Override
    @Contract("_ -> this")
    public BuildWorldCreatorImpl setReference(@Nullable File reference) {
        this.reference = reference;
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
        if (this.worldStorage.worldExists(this.worldName) || this.worldStorage.worldAndFolderExist(this.worldName)) {
            Messages.sendMessage(player, "worlds_world_exists");
            return;
        }

        if (this.reference != null && !this.reference.exists()) {
            Messages.sendMessage(player, "worlds_template_does_not_exist");
            return;
        }

        Messages.sendMessage(player, "worlds_world_creation_started",
                Map.entry("%world%", this.worldName),
                Map.entry("%type%", Messages.getString(Messages.getMessageKey(this.worldType), player))
        );

        if (this.reference != null) {
            File worldFile = new File(Bukkit.getWorldContainer(), this.worldName);
            FileUtils.copy(this.reference, worldFile);
        }

        this.buildWorld = createAndRegisterBuildWorld(player);
        if (generateBukkitWorld(true) != null) {
            teleportAfterCreation(player);
            Messages.sendMessage(player, "worlds_creation_finished");
        }
    }

    @Override
    public void importWorld(Player player, boolean teleport) {
        this.buildWorld = createAndRegisterBuildWorld(player);
        if (generateBukkitWorld(true) != null) {
            if (teleport) {
                teleportAfterCreation(player);
            }
        }
    }

    /**
     * Creates the {@link BuildWorld} object and registers it.
     *
     * @param player The player creating the world
     * @return The newly created {@link BuildWorld} instance
     */
    private BuildWorld createAndRegisterBuildWorld(Player player) {
        BuildWorldImpl buildWorld = new BuildWorldImpl(
                this.worldName,
                this.creator == null ? Builder.of(player) : this.creator,
                this.worldType,
                this.creationDate,
                this.isPrivate,
                this.customGenerator,
                this.folder
        );

        // Also store the world in the folder
        if (this.folder != null) {
            this.folder.addWorld(buildWorld);
        }

        buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        this.worldStorage.addBuildWorld(buildWorld);

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
    public World generateBukkitWorld(boolean checkVersion) {
        if (this.buildWorld == null) {
            throw new IllegalStateException("BuildWorld must be set before generating the Bukkit world.");
        }

        if (checkVersion && isDataVersionTooHigh()) {
            plugin.getLogger().warning(
                    "\"%s\" was created in a newer version of Minecraft (%s > %s). Skipping...".formatted(worldName, parseDataVersion(), getServerDataVersion())
            );
            return null;
        }

        WorldCreator worldCreator = createBukkitWorldCreator();
        World bukkitWorld = Bukkit.createWorld(worldCreator);

        if (bukkitWorld != null) {
            applyDefaultWorldSettings(bukkitWorld);
            applyPostGenerationSettings(bukkitWorld, this.buildWorld.getType());
            updateWorldDataVersion();
            saveGenerationData(bukkitWorld, this.buildWorld.getType(), this.customGenerator);
        }

        return bukkitWorld;
    }

    /**
     * Creates and configures a {@link WorldCreator} based on the specified {@link BuildWorldType}.
     *
     * @return A configured {@link WorldCreator}
     */
    private WorldCreator createBukkitWorldCreator() {
        WorldCreator worldCreator = new WorldCreator(this.worldName);

        ResolvedGeneratorSettings settings = resolveGeneratorSettings();
        BuildWorldType effectiveType = settings.type();
        CustomGenerator effectiveGenerator = settings.generator();

        if (effectiveGenerator != null && effectiveGenerator.chunkGenerator() != null) {
            worldCreator.generator(effectiveGenerator.chunkGenerator());
            plugin.getLogger().info("Using custom chunk generator '%s' for world '%s'".formatted(effectiveGenerator, this.worldName));
        }

        switch (effectiveType) {
            case CUSTOM:
                if (effectiveGenerator == null || effectiveGenerator.chunkGenerator() == null) {
                    plugin.getLogger().warning("World '%s' is type CUSTOM but no valid generator was found. Defaulting to NORMAL.".formatted(this.worldName));
                    worldCreator.type(WorldType.NORMAL);
                    worldCreator.generateStructures(true);
                    worldCreator.environment(World.Environment.NORMAL);
                }
                break;
            case VOID:
                worldCreator.type(WorldType.FLAT);
                worldCreator.generateStructures(false);
                worldCreator.generator(new VoidGenerator());
                break;
            case FLAT:
            case PRIVATE:
                worldCreator.type(WorldType.FLAT);
                worldCreator.generateStructures(false);
                break;
            case NETHER:
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.NETHER);
                break;
            case END:
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.THE_END);
                break;
            default: // NORMAL
                worldCreator.type(WorldType.NORMAL);
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.NORMAL);
                break;
        }

        return worldCreator;
    }

    /**
     * Resolves the effective world type and custom generator.
     * <p>
     * This is needed because {@link BuildWorldType#TEMPLATE} and {@link BuildWorldType#IMPORTED} can modify which {@link CustomGenerator} or {@link BuildWorldType} is used.
     *
     * @return The resolved generator settings
     */
    private ResolvedGeneratorSettings resolveGeneratorSettings() {
        BuildWorldType effectiveType = this.worldType;
        CustomGenerator effectiveGenerator = this.customGenerator;

        if (effectiveType == BuildWorldType.IMPORTED && effectiveGenerator != null) {
            try {
                effectiveType = BuildWorldType.valueOf(effectiveGenerator.chunkGeneratorName().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid custom generator name '%s' for imported world '%s'. Defaulting to NORMAL."
                        .formatted(effectiveGenerator.chunkGeneratorName(), this.worldName)
                );
                effectiveType = BuildWorldType.NORMAL;
            }
        } else if (effectiveType == BuildWorldType.TEMPLATE) {
            WorldGenerationData genData = loadGenerationData(this.worldName);
            switch (genData) {
                case PredefinedGeneratorData predefined -> effectiveType = predefined.type();
                case CustomGeneratorData custom -> {
                    effectiveGenerator = custom.getCustomGenerator(this.worldName);
                    if (effectiveGenerator == null) {
                        plugin.getLogger().warning(
                                "Custom generator '%s:%s' not found for template world '%s'. Defaulting to NORMAL type."
                                        .formatted(custom.pluginName(), custom.chunkGeneratorName(), this.worldName)
                        );
                        effectiveType = BuildWorldType.NORMAL;
                    } else {
                        // When a template resolves to a custom generator, treat its *type* as CUSTOM for generator selection.
                        effectiveType = BuildWorldType.CUSTOM;
                    }
                }
                default -> {
                    plugin.getLogger().warning("Failed to load world generation data for '%s'. Defaulting to NORMAL type.".formatted(this.worldName));
                    effectiveType = BuildWorldType.NORMAL;
                }
            }
        }

        return new ResolvedGeneratorSettings(effectiveType, effectiveGenerator);
    }

    /**
     * Applies standard server settings (difficulty, time, border, gamerules) to a newly created world.
     *
     * @param bukkitWorld The world to configure
     */
    private void applyDefaultWorldSettings(World bukkitWorld) {
        if (this.difficulty != null) {
            bukkitWorld.setDifficulty(this.difficulty);
        }
        if (this.time != null) {
            bukkitWorld.setTime(this.time);
        }
        if (this.worldBoarderSize != null) {
            bukkitWorld.getWorldBorder().setSize(Default.worldBoarderSize);
        }
        bukkitWorld.setKeepSpawnInMemory(true);
        Default.gameRules.forEach(gameRule -> applyGameRule(bukkitWorld, gameRule));
    }

    private static <T> void applyGameRule(World world, GameRuleEntry<T> entry) {
        try {
            world.setGameRule(entry.rule(), entry.value());
        } catch (Exception e) {
            BuildSystemPlugin.get().getLogger().warning("Failed to set gamerule " + entry.rule().getName() + " for world " + world.getName());
        }
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
                int voidBlockY = 64;
                bukkitWorld.getBlockAt(0, voidBlockY, 0).setType(XMaterial.GOLD_BLOCK.get());
                bukkitWorld.setSpawnLocation(0, voidBlockY + 1, 0);
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
        File levelFile = new File(new File(Bukkit.getWorldContainer(), this.worldName), LEVEL_DAT_FILE_NAME);
        if (!levelFile.exists()) {
            return -1;
        }

        try {
            CompoundTag level = new Nbt().fromFile(levelFile);
            CompoundTag data = level.get("Data");
            if (data == null) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse level.dat for world " + this.worldName + ": 'Data' tag not found.");
                return -1;
            }

            IntTag dataVersion = data.getInt("DataVersion");
            return dataVersion != null ? dataVersion.getValue() : -1;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse level.dat for world " + this.worldName, e);
            return -1;
        }
    }

    private void updateWorldDataVersion() {
        File levelFile = new File(new File(Bukkit.getWorldContainer(), this.worldName), LEVEL_DAT_FILE_NAME);
        if (!levelFile.exists()) {
            return;
        }

        try {
            Nbt nbt = new Nbt();
            CompoundTag level = nbt.fromFile(levelFile);
            CompoundTag data = level.get("Data");
            if (data == null) {
                plugin.getLogger().log(Level.WARNING, "Failed to update level.dat for world " + this.worldName + ": 'Data' tag not found.");
                return;
            }

            IntTag dataVersionTag = data.getInt("DataVersion");
            if (dataVersionTag == null) {
                plugin.getLogger().log(Level.INFO, "No 'DataVersion' tag found in level.dat for world " + worldName + ". Skipping update.");
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

    /**
     * Saves the world generation setting for a given {@link World} to a dedicated file within the world's folder.
     * <p>
     * If the {@link BuildWorldType} is {@link BuildWorldType#CUSTOM}, the specified chunk generator's name will be stored prefixed with "GENERATOR:". Otherwise, the
     * {@link BuildWorldType}'s enum name will be stored directly.
     * <p>
     * This method will only write the file if it does not already exist. If the file {@link #WORLD_TYPE_FILE_NAME} is already present in the world's folder, no action will be
     * taken, and the existing world generation setting will not be overwritten.
     *
     * @param world           The world for which to save the generation setting
     * @param worldType       The type representing the world's standard type
     * @param customGenerator The custom chunk generator, if {@code worldType} is {@link BuildWorldType#CUSTOM}. This parameter is ignored if {@code worldType} is not
     *                        {@link BuildWorldType#CUSTOM}
     */
    private void saveGenerationData(World world, BuildWorldType worldType, @Nullable CustomGenerator customGenerator) {
        renameIncorrectWorldTypeFile(world);

        Path path = Path.of(world.getWorldFolder() + File.separator + WORLD_TYPE_FILE_NAME);
        if (path.toFile().exists()) {
            return;
        }

        String contentToSave;
        if (worldType == BuildWorldType.CUSTOM) {
            if (customGenerator == null) {
                plugin.getLogger().warning("Attempted to save CUSTOM world type for world %s without a custom generator. Defaulting to NORMAL type.".formatted(world.getName()));
                contentToSave = BuildWorldType.NORMAL.name();
            } else {
                contentToSave = CUSTOM_GENERATOR_PREFIX + customGenerator;
            }
        } else {
            contentToSave = worldType.name();
        }

        try {
            Files.writeString(path, contentToSave, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to save world generation setting for world %s (type: %s, generator: %s)".formatted(world.getName(), worldType.name(), customGenerator),
                    e
            );
        }
    }

    // TODO: Remove this eventually
    // The old file was accidentally named incorrectly. Rename it to the correct name.
    private void renameIncorrectWorldTypeFile(World world) {
        Path parentDir = world.getWorldFolder().toPath();

        Path sourceFile = parentDir.resolve(".buildsystem-generator-date.txt");
        Path targetFile = parentDir.resolve(".buildsystem-generator-data.txt");

        try {
            if (Files.exists(sourceFile) && !Files.exists(targetFile)) {
                Files.move(sourceFile, targetFile);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to rename generator data file for world " + world.getName(), e);
        }
    }

    /**
     * Retrieves the world generation setting for a specific world identified by its name. It reads the setting from a dedicated file within the world's folder.
     * <p>
     * The file's content is interpreted: if it starts with "GENERATOR:", the rest is considered the custom chunk generator name. Otherwise, the content is parsed as a
     * {@link BuildWorldType} enum constant (case-insensitively).
     *
     * @param worldName The name of the world for which to retrieve the generation setting
     * @return A {@link WorldGenerationData} object representing either a {@link PredefinedGeneratorData} or a {@link CustomGeneratorData}, or {@code null} if the file does not
     * exist or an error occurs while loading
     */
    @Contract("_ -> new")
    private WorldGenerationData loadGenerationData(String worldName) {
        BuildWorldType defaultType = BuildWorldType.NORMAL;

        Path filePath = Path.of(plugin.getServer().getWorldContainer().getAbsolutePath(), worldName, WORLD_TYPE_FILE_NAME);
        if (!filePath.toFile().exists()) {
            return new PredefinedGeneratorData(defaultType);
        }

        String content;
        try {
            content = Files.readString(filePath).trim();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load world generation setting for world %s. Defaulting to %s.".formatted(worldName, defaultType), e);
            return new PredefinedGeneratorData(defaultType);
        }

        if (content.startsWith(CUSTOM_GENERATOR_PREFIX)) {
            String[] generatorData = content.substring(CUSTOM_GENERATOR_PREFIX.length()).trim().split(":");
            if (generatorData.length != 2) {
                plugin.getLogger().warning("Invalid custom generator name in file for world %s. Content: '%s'. Defaulting to %s.".formatted(worldName, content, defaultType));
                return new PredefinedGeneratorData(defaultType);
            }
            return new CustomGeneratorData(generatorData[0], generatorData[1]);
        } else {
            try {
                BuildWorldType type = BuildWorldType.valueOf(content.toUpperCase());
                return new PredefinedGeneratorData(type);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid BuildWorldType in file for world %s. Content: '%s'. Defaulting to %s.".formatted(worldName, content, defaultType));
                return new PredefinedGeneratorData(defaultType);
            }
        }
    }

    private void teleportAfterCreation(Player player) {
        BuildWorld worldToTeleport = Objects.requireNonNull(this.buildWorld);
        worldToTeleport.getUnloader().manageUnload();
        worldToTeleport.getTeleporter().teleport(player);
    }

    /**
     * Internal record to hold the resolved generator settings without mutating the class state.
     */
    private record ResolvedGeneratorSettings(BuildWorldType type, @Nullable CustomGenerator generator) {

    }

    /**
     * Represents the data loaded from the {@code .buildsystem-generator-data.txt} file. This can either be a predefined type or a custom generator.
     */
    interface WorldGenerationData {

        record PredefinedGeneratorData(BuildWorldType type) implements WorldGenerationData {

        }

        record CustomGeneratorData(String pluginName, String chunkGeneratorName) implements WorldGenerationData {

            @Nullable
            public CustomGenerator getCustomGenerator(String worldName) {
                return CustomGeneratorImpl.of("%s:%s".formatted(pluginName, chunkGeneratorName), worldName);
            }
        }
    }
}