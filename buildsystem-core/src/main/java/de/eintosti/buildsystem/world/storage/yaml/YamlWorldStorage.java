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
package de.eintosti.buildsystem.world.storage.yaml;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.builder.Builder;
import de.eintosti.buildsystem.world.data.WorldData;
import de.eintosti.buildsystem.world.data.WorldStatus;
import de.eintosti.buildsystem.world.data.WorldType;
import de.eintosti.buildsystem.world.generator.CustomGenerator;
import de.eintosti.buildsystem.world.storage.WorldStorage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YamlWorldStorage extends WorldStorage {

    private static final String WORLDS_KEY = "worlds";

    private final File file;
    private final FileConfiguration config;

    public YamlWorldStorage(BuildSystem plugin) {
        super(plugin);
        this.file = new File(plugin.getDataFolder(), "worlds.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadFile();
    }

    public void loadFile() {
        if (!file.exists()) {
            config.options().copyDefaults(true);
            saveFile();
            return;
        }

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, "Could not load worlds.yml file", e);
        }
    }

    public void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save worlds.yml file", e);
        }
    }

    @Override
    public void save(BuildWorld buildWorld) {
        config.set(WORLDS_KEY + "." + buildWorld.getName(), serializeWorld(buildWorld));
        saveFile();
    }

    @Override
    public void save(Collection<BuildWorld> buildWorlds) {
        buildWorlds.forEach(buildWorld -> config.set(WORLDS_KEY + "." + buildWorld.getName(), serializeWorld(buildWorld)));
        saveFile();
    }

    public @NotNull Map<String, Object> serializeWorld(BuildWorld buildWorld) {
        Map<String, Object> world = new HashMap<>();

        if (buildWorld.getCreator() != null) {
            world.put("creator", buildWorld.getCreator().toString());
        }
        world.put("type", buildWorld.getType().name());
        world.put("data", serializeWorldData(buildWorld.getData()));
        world.put("date", buildWorld.getCreationDate());
        world.put("builders", serializeBuilders(buildWorld.getBuilders()));
        if (buildWorld.getCustomGenerator() != null) {
            world.put("chunk-generator", buildWorld.getCustomGenerator().getName());
        }

        return world;
    }

    public @NotNull Map<String, Object> serializeWorldData(WorldData worldData) {
        return worldData.getAllData().entrySet().stream()
                .filter(entry -> entry.getValue().get() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getConfigFormat()));
    }

    public String serializeBuilders(List<Builder> builders) {
        StringBuilder builderList = new StringBuilder();
        for (Builder builder : builders) {
            builderList.append(";").append(builder.toString());
        }
        return builderList.length() > 0 ? builderList.substring(1) : builderList.toString();
    }

    @Override
    public Collection<BuildWorld> load() {
        ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY);
        if (section == null) {
            return new ArrayList<>();
        }

        Set<String> worlds = section.getKeys(false);
        if (worlds.isEmpty()) {
            return new ArrayList<>();
        }

        return worlds.stream()
                .map(this::loadWorld)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public BuildWorld loadWorld(String worldName) {
        Builder creator = parseCreator(worldName);
        WorldType worldType = config.isString("worlds." + worldName + ".type")
                ? WorldType.valueOf(config.getString("worlds." + worldName + ".type"))
                : WorldType.UNKNOWN;
        WorldData worldData = parseWorldData(worldName);
        long creationDate = config.isLong("worlds." + worldName + ".date")
                ? config.getLong("worlds." + worldName + ".date")
                : -1;
        List<Builder> builders = parseBuilders(worldName);
        String generatorName = config.getString("worlds." + worldName + ".chunk-generator");
        CustomGenerator customGenerator = new CustomGenerator(generatorName, parseChunkGenerator(worldName, generatorName));

        return new BuildWorld(
                worldName,
                creator,
                worldType,
                worldData,
                creationDate,
                customGenerator,
                builders
        );
    }

    private WorldData parseWorldData(String worldName) {
        final String path = WORLDS_KEY + "." + worldName + ".data";

        String customSpawn = config.getString(WORLDS_KEY + "." + worldName + ".spawn");
        String permission = config.getString(path + ".permission");
        String project = config.getString(path + ".project");

        Difficulty difficulty = Difficulty.valueOf(config.getString(path + ".difficulty", "PEACEFUL").toUpperCase(Locale.ROOT));
        XMaterial material = parseMaterial(path + ".material", worldName);
        WorldStatus worldStatus = WorldStatus.valueOf(config.getString(path + ".status"));

        boolean blockBreaking = config.getBoolean(path + ".block-breaking");
        boolean blockInteractions = config.getBoolean(path + ".block-interactions");
        boolean blockPlacement = config.getBoolean(path + ".block-placement");
        boolean buildersEnabled = config.getBoolean(path + ".builders-enabled");
        boolean explosions = config.getBoolean(path + ".explosions");
        boolean mobAi = config.getBoolean(path + ".mob-ai");
        boolean physics = config.getBoolean(path + ".physics");
        boolean privateWorld = config.getBoolean(path + ".private");

        long lastLoaded = config.getLong(path + ".last-loaded");
        long lastUnloaded = config.getLong(path + ".last-unloaded");
        long lastEdited = config.getLong(path + ".last-edited");

        return new WorldData(
                worldName, customSpawn, permission, project, difficulty, material, worldStatus, blockBreaking,
                blockInteractions, blockPlacement, buildersEnabled, explosions, mobAi, physics, privateWorld,
                lastLoaded, lastUnloaded, lastEdited
        );
    }

    private XMaterial parseMaterial(String path, String worldName) {
        String itemString = config.getString(path);
        if (itemString == null) {
            itemString = XMaterial.BEDROCK.name();
            plugin.getLogger().warning("Could not find material for \"" + worldName + "\". Defaulting to BEDROCK.");
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(itemString);
        if (xMaterial.isPresent()) {
            return xMaterial.get();
        } else {
            plugin.getLogger().warning("Unknown material found for \"" + worldName + "\" (" + itemString + ").");
            plugin.getLogger().warning("Defaulting back to BEDROCK.");
            return XMaterial.BEDROCK;
        }
    }

    private Builder parseCreator(String worldName) {
        final String creator = config.getString(WORLDS_KEY + "." + worldName + ".creator");
        final String oldCreatorIdPath = WORLDS_KEY + "." + worldName + ".creator-id";
        final String oldCreatorId = config.isString(oldCreatorIdPath)
                ? config.getString(oldCreatorIdPath)
                : null;

        // Previously, creator name & id were stored separately
        if (oldCreatorId != null) {
            if (creator == null || creator.equals("-")) {
                return null;
            }

            if (!oldCreatorId.equals("null")) {
                return Builder.of(UUID.fromString(oldCreatorId), creator);
            }

            return Builder.of(UUIDFetcher.getUUID(creator), creator);
        }

        return Builder.deserialize(creator);
    }

    private List<Builder> parseBuilders(String worldName) {
        List<Builder> builders = new ArrayList<>();

        if (config.isString(WORLDS_KEY + "." + worldName + ".builders")) {
            String buildersString = config.getString(WORLDS_KEY + "." + worldName + ".builders");
            if (buildersString != null && !buildersString.isEmpty()) {
                String[] splitBuilders = buildersString.split(";");
                for (String builder : splitBuilders) {
                    builders.add(Builder.deserialize(builder));
                }
            }
        }

        return builders;
    }

    /**
     * @author Ein_Jojo, einTosti
     */
    @Nullable
    private ChunkGenerator parseChunkGenerator(String worldName, String generatorName) {
        if (generatorName == null) {
            return null;
        }

        String[] generatorInfo = generatorName.split(":");
        if (generatorInfo.length == 1) {
            generatorInfo = new String[]{generatorInfo[0], generatorInfo[0]};
        }

        return getChunkGenerator(generatorInfo[0], generatorInfo[1], worldName);
    }

    /**
     * Gets the {@link ChunkGenerator} for the generation of a {@link BuildWorld} with {@link de.eintosti.buildsystem.world.data.WorldType#CUSTOM}
     *
     * @param generator   The plugin's (generator) name
     * @param generatorId Unique ID, if any, that was specified to indicate which generator was requested
     * @param worldName   Name of the world that the chunk generator should be applied to.
     */
    @Nullable
    public ChunkGenerator getChunkGenerator(String generator, String generatorId, String worldName) {
        if (generator == null) {
            return null;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(generator);
        if (plugin == null) {
            return null;
        }

        return plugin.getDefaultWorldGenerator(worldName, generatorId);
    }

    @Override
    public void delete(BuildWorld buildWorld) {
        delete(buildWorld.getName());
    }

    @Override
    public void delete(String worldKey) {
        config.set(WORLDS_KEY + "." + worldKey, null);
        saveFile();
    }
}