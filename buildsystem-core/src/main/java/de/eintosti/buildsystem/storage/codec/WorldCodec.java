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
package de.eintosti.buildsystem.storage.codec;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Codec} for {@link BuildWorld}s, mapping a world to and from its section. Since v4 the section is keyed by the
 * world's UUID and the name is carried as a {@code name} field (a rename is then a field update, not a key move).
 *
 * <p>The bulk of a world's state lives under the nested {@code data} section, whose keys mirror the property keys
 * registered in {@link WorldDataImpl} — the {@code DATA_*} constants here must stay in lock-step with those. Reads are
 * defensive: unknown enums fall back to safe defaults and a single unparseable entry surfaces as an exception for the
 * storage to skip rather than aborting the whole load.
 */
@NullMarked
public final class WorldCodec implements Codec<BuildWorld> {

    private static final String NAME = "name";
    private static final String UUID_KEY = "uuid";
    private static final String CREATOR = "creator";
    private static final String CREATOR_ID = "creator-id";
    private static final String TYPE = "type";
    private static final String DATE = "date";
    private static final String BUILDERS = "builders";
    private static final String CHUNK_GENERATOR = "chunk-generator";
    private static final String DATA = "data";

    private static final String DATA_SPAWN = "spawn";
    private static final String DATA_PERMISSION = "permission";
    private static final String DATA_PROJECT = "project";
    private static final String DATA_DIFFICULTY = "difficulty";
    private static final String DATA_MATERIAL = "material";
    private static final String DATA_ICON_SKULL_TEXTURE = "icon-skull-texture";
    private static final String DATA_STATUS = "status";
    private static final String DATA_BLOCK_BREAKING = "block-breaking";
    private static final String DATA_BLOCK_INTERACTIONS = "block-interactions";
    private static final String DATA_BLOCK_PLACEMENT = "block-placement";
    private static final String DATA_BUILDERS_ENABLED = "builders-enabled";
    private static final String DATA_EXPLOSIONS = "explosions";
    private static final String DATA_MOB_AI = "mob-ai";
    private static final String DATA_PHYSICS = "physics";
    private static final String DATA_PINNED = "pinned";
    private static final String DATA_VISIBILITY = "visibility";
    private static final String DATA_PRIVATE = "private";
    private static final String DATA_TIME_SINCE_BACKUP = "time-since-backup";
    private static final String DATA_LAST_LOADED = "last-loaded";
    private static final String DATA_LAST_UNLOADED = "last-unloaded";
    private static final String DATA_LAST_EDITED = "last-edited";

    private final BuildSystemPlugin plugin;

    public WorldCodec(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String key(BuildWorld value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, @Nullable Object> serialize(BuildWorld buildWorld) {
        Map<String, @Nullable Object> world = new HashMap<>();

        world.put(NAME, buildWorld.getName());
        world.put(UUID_KEY, buildWorld.getUniqueId().toString());
        Builders builders = buildWorld.getBuilders();
        if (builders.getCreator() != null) {
            world.put(CREATOR, builders.getCreator().toString());
        }
        world.put(TYPE, buildWorld.getType().name());
        world.put(DATA, serializeWorldData((WorldDataImpl) buildWorld.getData()));
        world.put(DATE, buildWorld.getCreation());
        world.put(BUILDERS, BuilderListCodec.format(builders.getAllBuilders()));
        if (buildWorld.getCustomGenerator() != null) {
            world.put(CHUNK_GENERATOR, buildWorld.getCustomGenerator().toString());
        }

        return world;
    }

    private Map<String, Object> serializeWorldData(WorldDataImpl worldData) {
        return worldData.getAllData().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().getConfigFormat()));
    }

    @Override
    public BuildWorldImpl deserialize(String key, ConfigurationSection section) {
        // v4 keys sections by UUID and carries the name as a field; fall back to the key for pre-migration safety.
        UUID uuid = UUID.fromString(key);
        String name = section.getString(NAME, key);
        Builder creator = parseCreator(name, section);
        BuildWorldType worldType = parseType(name, section);
        WorldDataImpl worldData = parseWorldData(name, section);
        long creationDate = section.isLong(DATE) ? section.getLong(DATE) : -1;
        List<Builder> builders = BuilderListCodec.parse(section.getString(BUILDERS));
        String generatorName = section.getString(CHUNK_GENERATOR);
        CustomGeneratorImpl customGenerator =
                generatorName != null ? CustomGeneratorImpl.of(generatorName, name) : null;

        return new BuildWorldImpl(
                WorldContext.fromPlugin(plugin),
                uuid,
                name,
                worldType,
                worldData,
                creator,
                builders,
                creationDate,
                customGenerator,
                null // The folder is set later.
                );
    }

    private WorldDataImpl parseWorldData(String worldName, ConfigurationSection section) {
        return new WorldDataBuilder(worldName)
                .withCustomSpawn(parseCustomSpawn(section))
                .withPermission(section.getString(DATA + "." + DATA_PERMISSION, "-"))
                .withProject(section.getString(DATA + "." + DATA_PROJECT, "-"))
                .withDifficulty(Difficulty.valueOf(section.getString(DATA + "." + DATA_DIFFICULTY, "PEACEFUL")
                        .toUpperCase(Locale.ROOT)))
                .withMaterial(parseMaterial(section, worldName))
                .withIconSkullTexture(section.getString(DATA + "." + DATA_ICON_SKULL_TEXTURE, ""))
                .withStatus(parseStatus(section, worldName))
                .withBlockBreaking(section.getBoolean(DATA + "." + DATA_BLOCK_BREAKING))
                .withBlockInteractions(section.getBoolean(DATA + "." + DATA_BLOCK_INTERACTIONS))
                .withBlockPlacement(section.getBoolean(DATA + "." + DATA_BLOCK_PLACEMENT))
                .withBuildersEnabled(section.getBoolean(DATA + "." + DATA_BUILDERS_ENABLED))
                .withExplosions(section.getBoolean(DATA + "." + DATA_EXPLOSIONS))
                .withMobAi(section.getBoolean(DATA + "." + DATA_MOB_AI))
                .withPhysics(section.getBoolean(DATA + "." + DATA_PHYSICS))
                .withPinned(section.getBoolean(DATA + "." + DATA_PINNED, false))
                .withVisibility(parseVisibility(section))
                .withTimeSinceBackup(section.getInt(DATA + "." + DATA_TIME_SINCE_BACKUP, 0))
                .withLastLoaded(section.getLong(DATA + "." + DATA_LAST_LOADED))
                .withLastUnloaded(section.getLong(DATA + "." + DATA_LAST_UNLOADED))
                .withLastEdited(section.getLong(DATA + "." + DATA_LAST_EDITED))
                .withPermissionOverrideEnabled(
                        () -> plugin.getConfigService().current().folder().overridePermissions())
                .withProjectOverrideEnabled(
                        () -> plugin.getConfigService().current().folder().overrideProjects())
                .build();
    }

    /**
     * Reads a world's custom spawn. It is serialized under {@code data.spawn} (its property key), but pre-property-map
     * files stored it at the top-level {@code spawn} key, so that location is the fallback.
     */
    private String parseCustomSpawn(ConfigurationSection section) {
        String dataSpawn = section.getString(DATA + "." + DATA_SPAWN);
        return dataSpawn != null ? dataSpawn : section.getString(DATA_SPAWN, "");
    }

    private BuildWorldType parseType(String worldName, ConfigurationSection section) {
        String raw = section.getString(TYPE);
        if (raw == null) {
            return BuildWorldType.UNKNOWN;
        }
        try {
            return BuildWorldType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            plugin.getLogger()
                    .warning("Unknown world type \"" + raw + "\" for \"" + worldName + "\". Defaulting to UNKNOWN.");
            return BuildWorldType.UNKNOWN;
        }
    }

    /**
     * Resolves a world's status from its persisted id, migrating pre-4.0 enum names (e.g. {@code NOT_STARTED}) to the
     * equivalent lower-case status id. Falls back to the registry default when the id is unknown.
     */
    private BuildWorldStatus parseStatus(ConfigurationSection section, String worldName) {
        WorldStatusRegistry registry = plugin.getWorldStatusRegistry();
        String raw = section.getString(DATA + "." + DATA_STATUS);
        if (raw == null) {
            return registry.getDefaultStatus();
        }
        String id = raw.toLowerCase(Locale.ROOT);
        return registry.getStatus(id).orElseGet(() -> {
            plugin.getLogger()
                    .warning("Unknown status \"" + raw + "\" for \"" + worldName + "\". Defaulting to "
                            + registry.getDefaultStatus().getId() + ".");
            return registry.getDefaultStatus();
        });
    }

    /**
     * Resolves a world's {@link Visibility}, reading the {@code visibility} key and migrating the pre-4.0
     * {@code private} boolean when the new key is absent.
     */
    private Visibility parseVisibility(ConfigurationSection section) {
        String raw = section.getString(DATA + "." + DATA_VISIBILITY);
        if (raw != null) {
            try {
                return Visibility.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the legacy private flag.
            }
        }
        return Visibility.matchVisibility(section.getBoolean(DATA + "." + DATA_PRIVATE));
    }

    private XMaterial parseMaterial(ConfigurationSection section, String worldName) {
        String itemString = section.getString(DATA + "." + DATA_MATERIAL);
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

    private @Nullable Builder parseCreator(String worldName, ConfigurationSection section) {
        final String creator = section.getString(CREATOR);

        // Previously, creator name & id were stored separately
        final String oldCreatorId = section.isString(CREATOR_ID) ? section.getString(CREATOR_ID) : null;
        if (oldCreatorId != null) {
            if (creator == null || creator.equals("-")) {
                return null;
            }

            if (!oldCreatorId.equals("null")) {
                return Builder.of(UUID.fromString(oldCreatorId), creator);
            }

            // Runs inside load()'s supplyAsync, so this off-main blocking lookup is safe.
            UUID creatorId = plugin.getPlayerLookupService().lookupUniqueIdBlocking(creator);
            if (creatorId == null) {
                return null;
            }

            return Builder.of(creatorId, creator);
        }

        return Builder.deserialize(creator);
    }
}
