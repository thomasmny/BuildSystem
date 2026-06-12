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

import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class GenerationDataStore {

    private static final String WORLD_TYPE_FILE_NAME = ".buildsystem-generator-data.txt";
    private static final String CUSTOM_GENERATOR_PREFIX = "GENERATOR:";

    private final Logger logger;
    private final File worldContainer;

    public GenerationDataStore(Logger logger, File worldContainer) {
        this.logger = logger;
        this.worldContainer = worldContainer;
    }

    /**
     * Saves the world generation setting for a given {@link World} to a dedicated file within the world's folder. If
     * the file already exists, no action is taken.
     */
    public void save(World world, BuildWorldType worldType, @Nullable CustomGenerator customGenerator) {
        renameIncorrectWorldTypeFile(world);

        Path path = Path.of(world.getWorldFolder() + File.separator + WORLD_TYPE_FILE_NAME);
        if (path.toFile().exists()) {
            return;
        }

        String contentToSave;
        if (worldType == BuildWorldType.CUSTOM) {
            if (customGenerator == null) {
                logger.warning(
                        "Attempted to save CUSTOM world type for world %s without a custom generator. Defaulting to NORMAL type."
                                .formatted(world.getName()));
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
            logger.log(
                    Level.WARNING,
                    "Failed to save world generation setting for world %s (type: %s, generator: %s)"
                            .formatted(world.getName(), worldType.name(), customGenerator),
                    e);
        }
    }

    @Contract("_ -> new")
    public WorldGenerationData load(String worldName) {
        BuildWorldType defaultType = BuildWorldType.NORMAL;

        Path filePath = Path.of(worldContainer.getAbsolutePath(), worldName, WORLD_TYPE_FILE_NAME);
        if (!filePath.toFile().exists()) {
            return new WorldGenerationData.PredefinedGeneratorData(defaultType);
        }

        String content;
        try {
            content = Files.readString(filePath).trim();
        } catch (IOException e) {
            logger.log(
                    Level.WARNING,
                    "Failed to load world generation setting for world %s. Defaulting to %s."
                            .formatted(worldName, defaultType),
                    e);
            return new WorldGenerationData.PredefinedGeneratorData(defaultType);
        }

        if (content.startsWith(CUSTOM_GENERATOR_PREFIX)) {
            String[] generatorData =
                    content.substring(CUSTOM_GENERATOR_PREFIX.length()).trim().split(":");
            if (generatorData.length != 2) {
                logger.warning("Invalid custom generator name in file for world %s. Content: '%s'. Defaulting to %s."
                        .formatted(worldName, content, defaultType));
                return new WorldGenerationData.PredefinedGeneratorData(defaultType);
            }
            return new WorldGenerationData.CustomGeneratorData(generatorData[0], generatorData[1]);
        } else {
            try {
                BuildWorldType type = BuildWorldType.valueOf(content.toUpperCase());
                return new WorldGenerationData.PredefinedGeneratorData(type);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid BuildWorldType in file for world %s. Content: '%s'. Defaulting to %s."
                        .formatted(worldName, content, defaultType));
                return new WorldGenerationData.PredefinedGeneratorData(defaultType);
            }
        }
    }

    // TODO: Remove this eventually — the old file was accidentally named incorrectly.
    private void renameIncorrectWorldTypeFile(World world) {
        Path parentDir = world.getWorldFolder().toPath();
        Path sourceFile = parentDir.resolve(".buildsystem-generator-date.txt");
        Path targetFile = parentDir.resolve(".buildsystem-generator-data.txt");

        try {
            if (Files.exists(sourceFile) && !Files.exists(targetFile)) {
                Files.move(sourceFile, targetFile);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to rename generator data file for world " + world.getName(), e);
        }
    }

    public sealed interface WorldGenerationData
            permits WorldGenerationData.PredefinedGeneratorData, WorldGenerationData.CustomGeneratorData {

        record PredefinedGeneratorData(BuildWorldType type) implements WorldGenerationData {}

        record CustomGeneratorData(String pluginName, String chunkGeneratorName) implements WorldGenerationData {

            @Nullable public CustomGenerator getCustomGenerator(String worldName) {
                return CustomGeneratorImpl.of("%s:%s".formatted(pluginName, chunkGeneratorName), worldName);
            }
        }
    }
}
