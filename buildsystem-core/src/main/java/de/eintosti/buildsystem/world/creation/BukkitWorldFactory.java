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
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.creation.GenerationDataStore.WorldGenerationData;
import de.eintosti.buildsystem.world.creation.generator.VoidGenerator;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BukkitWorldFactory {

    public enum VersionCheck {
        REQUIRED,
        SKIP
    }

    private static final String BUILT_IN_GENERATOR_PLUGIN = "BuildSystem";

    private final Logger logger;
    private final String worldName;
    private BuildWorldType worldType;

    private @Nullable CustomGenerator customGenerator;
    private final @Nullable Long seed;

    private final WorldDataVersionGuard versionGuard;
    private final GenerationDataStore generationDataStore;

    /**
     * Used when loading or regenerating an existing world.
     */
    public BukkitWorldFactory(Logger logger, BuildWorld buildWorld) {
        this(logger, buildWorld.getName(), buildWorld.getType(), buildWorld.getCustomGenerator(), null);
    }

    /**
     * Used when creating or importing a world. The creator applies the configured defaults once the world exists.
     */
    BukkitWorldFactory(
            Logger logger,
            String worldName,
            BuildWorldType worldType,
            @Nullable CustomGenerator customGenerator,
            @Nullable Long seed) {
        this.logger = logger;
        this.worldName = worldName;
        this.worldType = worldType;
        this.customGenerator = customGenerator;
        this.seed = seed;
        this.versionGuard = new WorldDataVersionGuard(logger, worldName);
        this.generationDataStore = new GenerationDataStore(logger);
    }

    public @Nullable World generate(VersionCheck versionCheck) {
        if (versionCheck == VersionCheck.REQUIRED && versionGuard.isDataVersionTooHigh()) {
            logger.warning("\"%s\" was created in a newer version of Minecraft (%s > %s). Skipping..."
                    .formatted(worldName, versionGuard.parseDataVersion(), versionGuard.getServerDataVersion()));
            return null;
        }

        World bukkitWorld;
        try {
            bukkitWorld = Bukkit.createWorld(configure(WorldNames.creator(worldName)));
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            // A namespaced world on Spigot, a stored name that is not a valid key, or a name Paper refuses because a
            // world with another key already has it: skip this world, not the rest of the load.
            logger.warning("\"%s\" cannot be loaded: %s. Skipping...".formatted(worldName, e.getMessage()));
            return null;
        }

        if (bukkitWorld != null) {
            generationDataStore.save(bukkitWorld, this.worldType, this.customGenerator);
        }

        return bukkitWorld;
    }

    private WorldCreator configure(WorldCreator worldCreator) {
        if (seed != null) {
            worldCreator.seed(seed);
        }
        BuildWorldType type = generationType(this.worldType, this.customGenerator);

        if (type == BuildWorldType.TEMPLATE) {
            switch (generationDataStore.load(worldName)) {
                case WorldGenerationData.PredefinedGeneratorData predefinedData -> {
                    type = predefinedData.type();
                }
                case WorldGenerationData.CustomGeneratorData customGeneratorData -> {
                    this.customGenerator = customGeneratorData.getCustomGenerator(worldName);
                    if (this.customGenerator == null) {
                        logger.warning("Custom generator '%s:%s' not found. Defaulting to NORMAL type."
                                .formatted(customGeneratorData.pluginName(), customGeneratorData.chunkGeneratorName()));
                        type = BuildWorldType.NORMAL;
                    }
                }
            }
        }

        if (this.customGenerator != null && this.customGenerator.chunkGenerator() != null) {
            worldCreator.generator(this.customGenerator.chunkGenerator());
            logger.info("Using custom chunk generator '%s' for world '%s'"
                    .formatted(this.customGenerator.toString(), worldName));
        }

        switch (type) {
            case VOID -> {
                worldCreator.type(WorldType.FLAT);
                worldCreator.generateStructures(false);
                worldCreator.generator(new VoidGenerator());
            }
            case FLAT, PRIVATE -> {
                worldCreator.type(WorldType.FLAT);
                worldCreator.generateStructures(false);
            }
            case NETHER -> {
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.NETHER);
            }
            case END -> {
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.THE_END);
            }
            default -> {
                worldCreator.type(WorldType.NORMAL);
                worldCreator.generateStructures(true);
                worldCreator.environment(World.Environment.NORMAL);
            }
        }

        this.worldType = type;
        return worldCreator;
    }

    /**
     * {@return the type to generate the world as} An imported world records BuildSystem's own generator as a
     * {@code BuildSystem:<type>} custom generator, which maps back to that type. A plugin's generator (Terra, say)
     * leaves the world {@code IMPORTED}, so it is generated by that plugin.
     */
    static BuildWorldType generationType(BuildWorldType type, @Nullable CustomGenerator customGenerator) {
        if (type != BuildWorldType.IMPORTED
                || customGenerator == null
                || !BUILT_IN_GENERATOR_PLUGIN.equals(customGenerator.pluginName())) {
            return type;
        }

        try {
            return BuildWorldType.valueOf(customGenerator.chunkGeneratorName().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /**
     * {@return the type the world was generated as} Differs from the stored type for a template, which takes the type
     * of the world it was copied from, and for a world imported with one of BuildSystem's own generators.
     */
    BuildWorldType generatedType() {
        return this.worldType;
    }
}
