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
package de.eintosti.buildsystem.storage.yaml;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlWorldStorage extends WorldStorageImpl {

    private static final String WORLDS_KEY = "worlds";

    private final File file;
    private final FileConfiguration config;

    public YamlWorldStorage(BuildSystemPlugin plugin, WorldServiceImpl worldService) {
        super(plugin, worldService);
        this.file = new File(plugin.getDataFolder(), "worlds.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public CompletableFuture<Void> save(BuildWorld buildWorld) {
        return CompletableFuture.runAsync(() -> {
            config.set(WORLDS_KEY + "." + buildWorld.getName(), serializeWorld(buildWorld));
            saveFile();
        });
    }

    @Override
    public CompletableFuture<Void> save(Collection<BuildWorld> buildWorlds) {
        return CompletableFuture.runAsync(() -> {
            buildWorlds.forEach(buildWorld -> config.set(WORLDS_KEY + "." + buildWorld.getName(), serializeWorld(buildWorld)));
            saveFile();
        });
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save worlds.yml file", e);
        }
    }

    public Map<String, Object> serializeWorld(BuildWorld buildWorld) {
        Map<String, Object> world = new HashMap<>();

        world.put("uuid", buildWorld.getUniqueId().toString());
        Builders builders = buildWorld.getBuilders();
        if (builders.getCreator() != null) {
            world.put("creator", builders.getCreator().toString());
        }
        world.put("type", buildWorld.getType().name());
        world.put("data", serializeWorldData(buildWorld.getData()));
        world.put("date", buildWorld.getCreation());
        world.put("builders", serializeBuilders(builders.getAllBuilders()));
        if (buildWorld.getCustomGenerator() != null) {
            world.put("chunk-generator", buildWorld.getCustomGenerator().toString());
        }

        return world;
    }

    private Map<String, Object> serializeWorldData(WorldData worldData) {
        return worldData.getAllData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getConfigFormat()));
    }

    private String serializeBuilders(Collection<Builder> builders) {
        StringBuilder builderList = new StringBuilder();
        for (Builder builder : builders) {
            builderList.append(";").append(builder);
        }
        return !builderList.isEmpty() ? builderList.substring(1) : builderList.toString();
    }

    @Override
    public CompletableFuture<Collection<BuildWorld>> load() {
        return CompletableFuture.supplyAsync(() ->
                loadWorldKeys().stream()
                        .map(this::loadWorld)
                        .collect(Collectors.toCollection(ArrayList::new))
        );
    }

    private Set<String> loadWorldKeys() {
        if (!file.exists()) {
            config.options().copyDefaults(true);
            saveFile();
            return Set.of();
        }

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, "Could not load worlds.yml file", e);
        }

        ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    @Contract("_ -> new")
    private BuildWorldImpl loadWorld(String worldName) {
        UUID uuid = config.isString("worlds." + worldName + ".uuid")
                ? UUID.fromString(config.getString("worlds." + worldName + ".uuid"))
                : UUID.randomUUID(); // Generate a new UUID if not present
        Builder creator = parseCreator(worldName);
        BuildWorldType worldType = config.isString("worlds." + worldName + ".type")
                ? BuildWorldType.valueOf(config.getString("worlds." + worldName + ".type"))
                : BuildWorldType.UNKNOWN;
        WorldDataImpl worldData = parseWorldData(worldName);
        long creationDate = config.isLong("worlds." + worldName + ".date")
                ? config.getLong("worlds." + worldName + ".date")
                : -1;
        List<Builder> builders = parseBuilders(worldName);
        String generatorName = config.getString("worlds." + worldName + ".chunk-generator");
        CustomGeneratorImpl customGenerator = generatorName != null
                ? CustomGeneratorImpl.of(generatorName, worldName)
                : null;

        return new BuildWorldImpl(
                uuid,
                worldName,
                worldType,
                worldData,
                creator,
                builders,
                creationDate,
                customGenerator,
                null // The folder will be set later
        );
    }

    @Contract("_ -> new")
    private WorldDataImpl parseWorldData(String worldName) {
        final String path = WORLDS_KEY + "." + worldName + ".data";
        return new WorldDataBuilder(worldName)
                .withCustomSpawn(config.getString(WORLDS_KEY + "." + worldName + ".spawn", ""))
                .withPermission(config.getString(path + ".permission", "-"))
                .withProject(config.getString(path + ".project", "-"))
                .withDifficulty(Difficulty.valueOf(config.getString(path + ".difficulty", "PEACEFUL").toUpperCase(Locale.ROOT)))
                .withMaterial(parseMaterial(path + ".material", worldName))
                .withStatus(BuildWorldStatus.valueOf(config.getString(path + ".status")))
                .withBlockBreaking(config.getBoolean(path + ".block-breaking"))
                .withBlockInteractions(config.getBoolean(path + ".block-interactions"))
                .withBlockPlacement(config.getBoolean(path + ".block-placement"))
                .withBuildersEnabled(config.getBoolean(path + ".builders-enabled"))
                .withExplosions(config.getBoolean(path + ".explosions"))
                .withMobAi(config.getBoolean(path + ".mob-ai"))
                .withPhysics(config.getBoolean(path + ".physics"))
                .withPrivateWorld(config.getBoolean(path + ".private"))
                .withTimeSinceBackup(config.getInt(path + ".time-since-backup", 0))
                .withLastLoaded(config.getLong(path + ".last-loaded"))
                .withLastUnloaded(config.getLong(path + ".last-unloaded"))
                .withLastEdited(config.getLong(path + ".last-edited"))
                .build();
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

    @Nullable
    private Builder parseCreator(String worldName) {
        final String creator = config.getString(WORLDS_KEY + "." + worldName + ".creator");

        // Previously, creator name & id were stored separately
        final String oldCreatorIdPath = WORLDS_KEY + "." + worldName + ".creator-id";
        final String oldCreatorId = config.isString(oldCreatorIdPath) ? config.getString(oldCreatorIdPath) : null;
        if (oldCreatorId != null) {
            if (creator == null || creator.equals("-")) {
                return null;
            }

            if (!oldCreatorId.equals("null")) {
                return Builder.of(UUID.fromString(oldCreatorId), creator);
            }

            UUID creatorId = UUIDFetcher.getUUID(creator);
            if (creatorId == null) {
                return null;
            }

            return Builder.of(creatorId, creator);
        }

        return Builder.deserialize(creator);
    }

    private List<Builder> parseBuilders(String worldName) {
        List<Builder> builders = new ArrayList<>();

        if (config.isString(WORLDS_KEY + "." + worldName + ".builders")) {
            String buildersString = config.getString(WORLDS_KEY + "." + worldName + ".builders");
            if (buildersString != null && !buildersString.isEmpty()) {
                String[] splitBuilders = buildersString.split(";");
                for (String serializedBuilder : splitBuilders) {
                    Builder builder = Builder.deserialize(serializedBuilder);
                    if (builder == null) {
                        plugin.getLogger().warning("Could not deserialize builder: " + serializedBuilder + " for world: " + worldName);
                        continue;
                    }
                    builders.add(builder);
                }
            }
        }

        return builders;
    }

    @Override
    public CompletableFuture<Void> delete(BuildWorld buildWorld) {
        return delete(buildWorld.getName());
    }

    @Override
    public CompletableFuture<Void> delete(String worldKey) {
        return CompletableFuture.runAsync(() -> {
            config.set(WORLDS_KEY + "." + worldKey, null);
            saveFile();
        });
    }
}