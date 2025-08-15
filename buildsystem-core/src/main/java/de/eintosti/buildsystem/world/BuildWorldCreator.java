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
package de.eintosti.buildsystem.world;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.version.util.MinecraftVersion;
import de.eintosti.buildsystem.world.builder.Builder;
import de.eintosti.buildsystem.world.data.WorldType;
import de.eintosti.buildsystem.world.generator.CustomGenerator;
import de.eintosti.buildsystem.world.generator.voidgenerator.DeprecatedVoidGenerator;
import de.eintosti.buildsystem.world.generator.voidgenerator.ModernVoidGenerator;
import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.io.CompressionType;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Trichtern
 * @since 2.21.0
 */
public class BuildWorldCreator {

    private final BuildSystem plugin;
    private final WorldService worldService;

    private String worldName;
    private Builder creator;
    private boolean privateWorld = false;
    private WorldType worldType = WorldType.NORMAL;
    private CustomGenerator customGenerator = null;
    private long creationDate = System.currentTimeMillis();
    private String template = null;
    private Difficulty difficulty;

    public BuildWorldCreator(BuildSystem plugin, @NotNull String name) {
        this.plugin = plugin;
        this.worldService = plugin.getWorldService();

        setName(name);
        setDifficulty(plugin.getConfigValues().getWorldDifficulty());
    }

    public BuildWorldCreator(BuildSystem plugin, BuildWorld buildWorld) {
        this(plugin, buildWorld.getName());

        setDifficulty(buildWorld.getData().difficulty().get());
        setCreationDate(buildWorld.getCreationDate());
        setType(buildWorld.getType());
        setCustomGenerator(buildWorld.getCustomGenerator());
        setPrivate(buildWorld.getData().privateWorld().get());
    }

    public BuildWorldCreator setName(String name) {
        this.worldName = name;
        return this;
    }

    public BuildWorldCreator setCreator(Builder creator) {
        this.creator = creator;
        return this;
    }

    public BuildWorldCreator setTemplate(String template) {
        this.template = ChatColor.stripColor(template);
        return this;
    }

    public BuildWorldCreator setType(WorldType type) {
        this.worldType = type;
        return this;
    }

    public BuildWorldCreator setCustomGenerator(CustomGenerator customGenerator) {
        this.customGenerator = customGenerator;
        return this;
    }

    public BuildWorldCreator setPrivate(boolean privateWorld) {
        this.privateWorld = privateWorld;
        return this;
    }

    public BuildWorldCreator setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public BuildWorldCreator setCreationDate(long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * Depending on the {@link BuildWorld}'s {@link WorldType}, the corresponding {@link World} will be generated in a different way. Then, if the creation of the world was
     * successful and the config is set accordingly, the player is teleported to the world.
     *
     * @param player The player who is creating the world
     */
    public void createWorld(Player player) {
        switch (worldType) {
            case TEMPLATE:
                createTemplateWorld(player);
                break;
            default:
                createPredefinedOrCustomWorld(player);
                break;
        }
    }

    private BuildWorld createBuildWorldObject(Player player) {
        BuildWorld buildWorld = new BuildWorld(
                worldName,
                creator == null ? Builder.of(player) : creator,
                worldType,
                creationDate,
                privateWorld,
                customGenerator
        );
        buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        return buildWorld;
    }

    /**
     * Generate a {@link BuildWorld} with a predefined or custom generator.
     *
     * @param player The player who is creating the world
     */
    private void createPredefinedOrCustomWorld(Player player) {
        if (worldService.worldExists(player, worldName)) {
            return;
        }

        BuildWorld buildWorld = createBuildWorldObject(player);
        worldService.addBuildWorld(buildWorld);

        Messages.sendMessage(player, "worlds_world_creation_started",
                new AbstractMap.SimpleEntry<>("%world%", worldName),
                new AbstractMap.SimpleEntry<>("%type%", worldType.getName(player))
        );
        finishPreparationsAndGenerate(buildWorld);
        teleportAfterCreation(player);
        Messages.sendMessage(player, "worlds_creation_finished");
    }

    /**
     * Imports an existing world as a {@link BuildWorld}.
     *
     * @param player   The player who is importing the world
     * @param teleport Should the player be teleported to the world after importing is finished
     */
    public void importWorld(Player player, boolean teleport) {
        BuildWorld buildWorld = createBuildWorldObject(player);
        worldService.addBuildWorld(buildWorld);
        finishPreparationsAndGenerate(buildWorld);
        if (teleport) {
            teleportAfterCreation(player);
        }
    }

    /**
     * Generate a {@link BuildWorld} with a template.
     *
     * @param player The player who is creating the world
     */
    private void createTemplateWorld(Player player) {
        if (worldService.worldExists(player, worldName)) {
            return;
        }

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        File templateFile = new File(plugin.getDataFolder() + File.separator + "templates" + File.separator + template);
        if (!templateFile.exists()) {
            Messages.sendMessage(player, "worlds_template_does_not_exist");
            return;
        }

        BuildWorld buildWorld = createBuildWorldObject(player);
        worldService.addBuildWorld(buildWorld);

        Messages.sendMessage(player, "worlds_template_creation_started",
                new AbstractMap.SimpleEntry<>("%world%", worldName),
                new AbstractMap.SimpleEntry<>("%template%", template)
        );
        FileUtils.copy(templateFile, worldFile);
        Bukkit.createWorld(WorldCreator.name(worldName)
                .type(org.bukkit.WorldType.FLAT)
                .generateStructures(false));
        teleportAfterCreation(player);
        Messages.sendMessage(player, "worlds_creation_finished");
    }

    /**
     * Certain {@link WorldType}s require modifications to the world after its generation.
     *
     * @param buildWorld The build world object
     */
    private void finishPreparationsAndGenerate(BuildWorld buildWorld) {
        WorldType worldType = buildWorld.getType();
        World bukkitWorld = generateBukkitWorld();
        if (bukkitWorld == null) {
            return;
        }

        switch (worldType) {
            case VOID:
                if (plugin.getConfigValues().isVoidBlock()) {
                    bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
                }
                bukkitWorld.setSpawnLocation(0, 65, 0);
                break;
            case FLAT:
                int y = MinecraftVersion.getCurrent().isEqualOrHigherThan(MinecraftVersion.CAVES_18) ? -60 : 4;
                bukkitWorld.setSpawnLocation(0, y, 0);
                break;
            default:
                break;
        }
    }

    @Nullable
    public World generateBukkitWorld() {
        return generateBukkitWorld(true);
    }

    /**
     * Generate the {@link World} linked to a {@link BuildWorld}.
     *
     * @param checkVersion Should the world version be checked
     * @return The world object
     */
    @Nullable
    public World generateBukkitWorld(boolean checkVersion) {
        if (checkVersion && isHigherVersion()) {
            plugin.getLogger()
                    .warning(String.format(Locale.ROOT,
                            "\"%s\" was created in a newer version of Minecraft (%s > %s). Skipping...",
                            worldName, parseDataVersion(), plugin.getCraftBukkitVersion().getDataVersion()
                    ));
            return null;
        }

        WorldCreator worldCreator = new WorldCreator(worldName);
        org.bukkit.WorldType bukkitWorldType;

        switch (worldType) {
            case VOID:
                worldCreator.generateStructures(false);
                bukkitWorldType = org.bukkit.WorldType.FLAT;
                MinecraftVersion minecraftVersion = MinecraftVersion.getCurrent();
                if (minecraftVersion.isEqualOrHigherThan(MinecraftVersion.CAVES_17)) {
                    worldCreator.generator(new ModernVoidGenerator());
                } else if (minecraftVersion.isEqualOrHigherThan(MinecraftVersion.AQUATIC_13)) {
                    worldCreator.generator(new DeprecatedVoidGenerator());
                } else {
                    worldCreator.generatorSettings("2;0;1");
                }
                break;

            case FLAT:
            case PRIVATE:
                worldCreator.generateStructures(false);
                bukkitWorldType = org.bukkit.WorldType.FLAT;
                break;

            case NETHER:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(World.Environment.NETHER);
                break;

            case END:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(World.Environment.THE_END);
                break;

            case CUSTOM:
                if (customGenerator != null) {
                    worldCreator.generator(customGenerator.getChunkGenerator());
                }
                // Drop through

            default:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(World.Environment.NORMAL);
                break;
        }
        worldCreator.type(bukkitWorldType);

        World bukkitWorld = Bukkit.createWorld(worldCreator);
        if (bukkitWorld != null) {
            ConfigValues configValues = plugin.getConfigValues();
            bukkitWorld.setDifficulty(difficulty);
            bukkitWorld.setTime(configValues.getNoonTime());
            bukkitWorld.getWorldBorder().setSize(configValues.getWorldBorderSize());
            bukkitWorld.setKeepSpawnInMemory(configValues.isTeleportAfterCreation());
            configValues.getDefaultGameRules().forEach(bukkitWorld::setGameRuleValue);
        }

        updateDataVersion();
        return bukkitWorld;
    }

    /**
     * Once a chunk has been loaded in a newer version of Minecraft, then it cannot be loaded in an older version again. Paper allows the server admin to bypass this check with
     * {@code }, so we do as well.
     *
     * @return {@code true} if the world was generated in a higher Minecraft version, otherwise {@code false}
     */
    public boolean isHigherVersion() {
        if (Boolean.getBoolean("Paper.ignoreWorldDataVersion")) {
            return false;
        }
        return parseDataVersion() > plugin.getCraftBukkitVersion().getDataVersion();
    }

    /**
     * Parses the world's data version, as stored in {@code level.dat}.
     *
     * @return The world's data version if found, otherwise -1 if unable to parse
     * @see <a href="https://minecraft.wiki/wiki/Data_version">Data version</a>
     */
    public int parseDataVersion() {
        File levelFile = new File(Bukkit.getWorldContainer() + File.separator + worldName, "level.dat");
        if (!levelFile.exists()) {
            return -1;
        }

        try {
            CompoundTag level = new Nbt().fromFile(levelFile);
            CompoundTag data = level.get("Data");
            IntTag dataVersion = data.getInt("DataVersion");

            return dataVersion != null ? dataVersion.getValue() : -1;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * The {@code level.dat} file is not updated when a newer Minecraft version loads chunks, making the world not loadable. Therefore, manually sets the world's
     * {@code DataVersion} to the current server version, if lower.
     */
    private void updateDataVersion() {
        File levelFile = new File(Bukkit.getWorldContainer() + File.separator + worldName, "level.dat");
        if (!levelFile.exists()) {
            return;
        }

        try {
            Nbt nbt = new Nbt();
            CompoundTag level = nbt.fromFile(levelFile);
            CompoundTag data = level.get("Data");
            IntTag dataVersion = data.getInt("DataVersion");
            if (dataVersion == null) {
                return;
            }

            int serverVersion = plugin.getCraftBukkitVersion().getDataVersion();
            if (dataVersion.getValue() < serverVersion) {
                dataVersion.setValue(serverVersion);
                nbt.toFile(level, levelFile, CompressionType.GZIP);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void teleportAfterCreation(Player player) {
        if (!plugin.getConfigValues().isTeleportAfterCreation()) {
            return;
        }

        BuildWorld buildWorld = worldService.getBuildWorld(worldName);
        if (buildWorld == null) {
            return;
        }

        buildWorld.manageUnload();
        worldService.teleport(player, buildWorld);
    }
}