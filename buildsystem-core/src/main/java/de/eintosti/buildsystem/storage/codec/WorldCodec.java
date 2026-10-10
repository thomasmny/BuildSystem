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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.storage.codec.FieldCodec.Field;
import de.eintosti.buildsystem.storage.codec.FieldCodec.Reader;
import de.eintosti.buildsystem.util.MaterialUtils;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Codec} for {@link BuildWorld}s, mapping a world to and from its section. Since v4 the section is keyed by the
 * world's UUID and the name is carried as a {@code name} field (a rename is then a field update, not a key move).
 *
 * <p>The settings live under the nested {@code data} section, one field per {@link WorldDataSchema} key. Reads are
 * defensive: unknown enums fall back to safe defaults and a single unparseable entry surfaces as an exception for the
 * storage to skip rather than aborting the whole load.
 */
@NullMarked
public final class WorldCodec implements Codec<BuildWorld> {

    private final WorldContext context;
    private final PlayerLookupService playerLookup;
    private final FieldCodec<BuildWorld, ReadWorld> fields;

    public WorldCodec(WorldContext context, PlayerLookupService playerLookup) {
        this.context = context;
        this.playerLookup = playerLookup;
        this.fields = new FieldCodec<>(fields());
    }

    private List<Field<BuildWorld, ReadWorld, ?>> fields() {
        List<Field<BuildWorld, ReadWorld, ?>> fields = new ArrayList<>();
        fields.add(FieldCodec.writeOnly("name", BuildWorld::getName));
        fields.add(FieldCodec.writeOnly("uuid", world -> world.getUniqueId().toString()));
        fields.add(new Field<>(
                "creator",
                world -> {
                    Builder creator = world.getBuilders().getCreator();
                    return creator == null ? null : creator.toString();
                },
                this::readCreator,
                (read, creator) -> read.creator = creator));
        fields.add(
                new Field<>("type", world -> world.getType().name(), this::readType, (read, type) -> read.type = type));
        fields.add(new Field<>(
                "date",
                BuildWorld::getCreation,
                (section, key) -> section.isLong(key) ? section.getLong(key) : -1L,
                (read, date) -> read.date = date));
        fields.add(new Field<>(
                "builders",
                world -> BuilderListCodec.format(world.getBuilders().getAllBuilders()),
                (section, key) -> BuilderListCodec.parse(section.getString(key)),
                (read, builders) -> read.builders = builders));
        fields.add(new Field<>(
                "chunk-generator",
                world -> world.getCustomGenerator() == null
                        ? null
                        : world.getCustomGenerator().toString(),
                WorldCodec::readGenerator,
                (read, generator) -> read.generator = generator));

        Map<WorldDataKey<?>, Field<BuildWorld, ReadWorld, ?>> special = new HashMap<>();
        special.put(WorldDataKey.CUSTOM_SPAWN, dataField(WorldDataKey.CUSTOM_SPAWN, WorldCodec::readSpawn));
        special.put(WorldDataKey.DIFFICULTY, dataField(WorldDataKey.DIFFICULTY, this::readDifficulty));
        special.put(WorldDataKey.MATERIAL, dataField(WorldDataKey.MATERIAL, this::readMaterial));
        special.put(WorldDataKey.VISIBILITY, dataField(WorldDataKey.VISIBILITY, WorldCodec::readVisibility));
        for (PhysicsCategory category : PhysicsCategory.values()) {
            // An absent exception takes the configured default, not the schema's.
            special.put(
                    category.key(),
                    dataField(category.key(), (section, key) -> section.getBoolean(key, physicsDefault(category))));
        }

        fields.add(dataField(WorldDataKey.STATUS, this::readStatus));
        for (WorldDataKey<?> key : WorldDataSchema.keys()) {
            if (!key.equals(WorldDataKey.STATUS)) {
                fields.add(special.containsKey(key) ? special.get(key) : plainField(key));
            }
        }
        return fields;
    }

    private static <V> Field<BuildWorld, ReadWorld, V> dataField(WorldDataKey<V> key, Reader<V> reader) {
        return new Field<>(
                "data." + key.id(),
                world -> WorldDataSchema.toYaml(
                        ((WorldDataImpl) world.getData()).storedValues().get(key)),
                reader,
                (read, value) -> read.data.set(key, value));
    }

    /**
     * A string, boolean or number field, falling back to the schema's value when the key is absent.
     */
    private static <V> Field<BuildWorld, ReadWorld, V> plainField(WorldDataKey<V> key) {
        Class<V> type = key.type();
        Object fallback = WorldDataSchema.fallback(key);
        if (type == Boolean.class) {
            return dataField(key, (section, path) -> type.cast(section.getBoolean(path, (Boolean) fallback)));
        } else if (type == Integer.class) {
            return dataField(key, (section, path) -> type.cast(section.getInt(path, (Integer) fallback)));
        } else if (type == Long.class) {
            return dataField(key, (section, path) -> type.cast(section.getLong(path, (Long) fallback)));
        } else if (type == String.class) {
            return dataField(key, (section, path) -> type.cast(section.getString(path, (String) fallback)));
        }
        throw new IllegalStateException("No reader for world data key " + key.id() + " of type " + type);
    }

    @Override
    public String key(BuildWorld value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, Object> serialize(BuildWorld buildWorld) {
        return fields.serialize(buildWorld);
    }

    @Override
    public BuildWorldImpl deserialize(String key, ConfigurationSection section) {
        String name = worldName(section);
        ReadWorld read = new ReadWorld(
                WorldDataSchema.create(name, context.statusRegistry().getDefault()));
        fields.read(section, read);
        return new BuildWorldImpl(
                context,
                UUID.fromString(key),
                name,
                read.type,
                read.data,
                read.creator,
                read.builders,
                read.date,
                read.generator,
                null // The folder is set later.
                );
    }

    /**
     * The fields read from a world's section, until the world is built from them.
     */
    private static final class ReadWorld {

        private final WorldDataImpl data;
        private @Nullable Builder creator;
        private BuildWorldType type = BuildWorldType.UNKNOWN;
        private long date = -1;
        private List<Builder> builders = List.of();
        private @Nullable CustomGeneratorImpl generator;

        private ReadWorld(WorldDataImpl data) {
            this.data = data;
        }
    }

    /**
     * {@return the world's name} Falls back to the section's key for files written before the name was a field.
     */
    private static String worldName(ConfigurationSection section) {
        return section.getString("name", section.getName());
    }

    private boolean physicsDefault(PhysicsCategory category) {
        return context.configService().current().world().defaults().physicsException(category);
    }

    private static @Nullable CustomGeneratorImpl readGenerator(ConfigurationSection section, String key) {
        String generator = section.getString(key);
        return generator == null ? null : CustomGeneratorImpl.stored(generator, worldName(section));
    }

    /**
     * Reads a world's custom spawn. Files from before the settings moved under {@code data} stored it at the top-level
     * {@code spawn} key, so that is the fallback.
     */
    private static String readSpawn(ConfigurationSection section, String key) {
        String dataSpawn = section.getString(key);
        return dataSpawn != null ? dataSpawn : section.getString(WorldDataKey.CUSTOM_SPAWN.id(), "");
    }

    private BuildWorldType readType(ConfigurationSection section, String key) {
        String raw = section.getString(key);
        if (raw == null) {
            return BuildWorldType.UNKNOWN;
        }

        try {
            return BuildWorldType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            context.logger()
                    .warning("Unknown world type \"" + raw + "\" for \"" + worldName(section)
                            + "\". Defaulting to UNKNOWN.");
            return BuildWorldType.UNKNOWN;
        }
    }

    /**
     * Resolves a world's {@link Difficulty}, falling back to {@link Difficulty#PEACEFUL} when the persisted value is
     * unknown. Like the other enums, an unparseable difficulty must not abort the world's load.
     */
    private Difficulty readDifficulty(ConfigurationSection section, String key) {
        String raw = section.getString(key, Difficulty.PEACEFUL.name());
        try {
            return Difficulty.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            context.logger()
                    .warning("Unknown difficulty \"" + raw + "\" for \"" + worldName(section)
                            + "\". Defaulting to PEACEFUL.");
            return Difficulty.PEACEFUL;
        }
    }

    /**
     * Resolves a world's status from its persisted id, migrating pre-4.0 enum names (e.g. {@code NOT_STARTED}) to the
     * equivalent lower-case status id. Falls back to the registry default when the id is unknown.
     */
    private BuildWorldStatus readStatus(ConfigurationSection section, String key) {
        WorldStatusRegistry registry = context.statusRegistry();
        String raw = section.getString(key);
        if (raw == null) {
            return registry.getDefault();
        }

        String id = raw.toLowerCase(Locale.ROOT);
        return registry.get(id).orElseGet(() -> {
            context.logger()
                    .warning("Unknown status \"" + raw + "\" for \"" + worldName(section) + "\". Defaulting to "
                            + registry.getDefault().getId() + ".");
            return registry.getDefault();
        });
    }

    /**
     * Resolves a world's {@link Visibility}, reading the {@code visibility} key and migrating the pre-4.0
     * {@code private} boolean when the new key is absent.
     */
    private static Visibility readVisibility(ConfigurationSection section, String key) {
        String raw = section.getString(key);
        if (raw != null) {
            try {
                return Visibility.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the legacy private flag.
            }
        }
        return Visibility.matchVisibility(section.getBoolean("data.private"));
    }

    private Material readMaterial(ConfigurationSection section, String key) {
        String itemString = section.getString(key);
        if (itemString == null) {
            context.logger()
                    .warning("Could not find material for \"" + worldName(section) + "\". Defaulting to BEDROCK.");
            return Material.BEDROCK;
        }

        Material material = MaterialUtils.match(itemString);
        if (material == null) {
            context.logger().warning("Unknown material found for \"" + worldName(section) + "\" (" + itemString + ").");
            context.logger().warning("Defaulting back to BEDROCK.");
            return Material.BEDROCK;
        }
        return material;
    }

    private @Nullable Builder readCreator(ConfigurationSection section, String key) {
        final String creator = section.getString(key);

        // Previously, creator name & id were stored separately
        final String oldCreatorId = section.isString("creator-id") ? section.getString("creator-id") : null;
        if (oldCreatorId != null) {
            if (creator == null || creator.equals("-")) {
                return null;
            }

            if (!oldCreatorId.equals("null")) {
                return Builder.of(UUID.fromString(oldCreatorId), creator);
            }

            // Runs inside load()'s supplyAsync, so this off-main blocking lookup is safe.
            UUID creatorId = playerLookup.lookupUniqueIdBlocking(creator);
            if (creatorId == null) {
                return null;
            }

            return Builder.of(creatorId, creator);
        }

        return Builder.deserialize(creator);
    }
}
