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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.world.creation.GenerationDataStore.WorldGenerationData;
import de.eintosti.buildsystem.world.creation.generator.VoidGenerator;
import de.eintosti.buildsystem.world.menu.GameRuleEntry;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
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

    private final BuildSystemPlugin plugin;
    private final String worldName;
    private BuildWorldType worldType;

    @Nullable
    private CustomGenerator customGenerator;

    @Nullable
    private final Difficulty difficulty;

    @Nullable
    private final Integer time;

    @Nullable
    private final Integer worldBorderSize;

    private final WorldDataVersionGuard versionGuard;
    private final GenerationDataStore generationDataStore;

    /**
     * Used when loading or regenerating an existing world (no default settings to apply).
     */
    public BukkitWorldFactory(BuildSystemPlugin plugin, BuildWorld buildWorld) {
        this.plugin = plugin;
        this.worldName = buildWorld.getName();
        this.worldType = buildWorld.getType();
        this.customGenerator = buildWorld.getCustomGenerator();
        this.difficulty = null;
        this.time = null;
        this.worldBorderSize = null;
        this.versionGuard = new WorldDataVersionGuard(plugin.getLogger(), worldName);
        this.generationDataStore = new GenerationDataStore(plugin.getLogger(), Bukkit.getWorldContainer());
    }

    /**
     * Used when creating a new world with defaults from plugin config.
     */
    BukkitWorldFactory(
            BuildSystemPlugin plugin,
            String worldName,
            BuildWorldType worldType,
            @Nullable CustomGenerator customGenerator,
            @Nullable Difficulty difficulty,
            @Nullable Integer time,
            @Nullable Integer worldBorderSize) {
        this.plugin = plugin;
        this.worldName = worldName;
        this.worldType = worldType;
        this.customGenerator = customGenerator;
        this.difficulty = difficulty;
        this.time = time;
        this.worldBorderSize = worldBorderSize;
        this.versionGuard = new WorldDataVersionGuard(plugin.getLogger(), worldName);
        this.generationDataStore = new GenerationDataStore(plugin.getLogger(), Bukkit.getWorldContainer());
    }

    @Nullable
    public World generate(VersionCheck versionCheck) {
        if (versionCheck == VersionCheck.REQUIRED && versionGuard.isDataVersionTooHigh()) {
            plugin.getLogger()
                    .warning("\"%s\" was created in a newer version of Minecraft (%s > %s). Skipping..."
                            .formatted(
                                    worldName, versionGuard.parseDataVersion(), versionGuard.getServerDataVersion()));
            return null;
        }

        WorldCreator worldCreator = createWorldCreator();
        World bukkitWorld = Bukkit.createWorld(worldCreator);

        if (bukkitWorld != null) {
            applyDefaultWorldSettings(bukkitWorld);
            applyPostGenerationSettings(bukkitWorld, this.worldType);
            versionGuard.updateWorldDataVersion();
            generationDataStore.save(bukkitWorld, this.worldType, this.customGenerator);
        }

        return bukkitWorld;
    }

    private WorldCreator createWorldCreator() {
        WorldCreator worldCreator = new WorldCreator(worldName);
        BuildWorldType type = this.worldType;

        if (type == BuildWorldType.IMPORTED && this.customGenerator != null) {
            type = BuildWorldType.valueOf(
                    this.customGenerator.chunkGeneratorName().toUpperCase(Locale.ROOT));
        }

        if (type == BuildWorldType.TEMPLATE) {
            switch (generationDataStore.load(worldName)) {
                case WorldGenerationData.PredefinedGeneratorData predefinedData -> {
                    type = predefinedData.type();
                }
                case WorldGenerationData.CustomGeneratorData customGeneratorData -> {
                    this.customGenerator = customGeneratorData.getCustomGenerator(worldName);
                    if (this.customGenerator == null) {
                        plugin.getLogger()
                                .warning("Custom generator '%s:%s' not found. Defaulting to NORMAL type."
                                        .formatted(
                                                customGeneratorData.pluginName(),
                                                customGeneratorData.chunkGeneratorName()));
                        type = BuildWorldType.NORMAL;
                    }
                }
            }
        }

        if (this.customGenerator != null && this.customGenerator.chunkGenerator() != null) {
            worldCreator.generator(this.customGenerator.chunkGenerator());
            plugin.getLogger()
                    .info("Using custom chunk generator '%s' for world '%s'"
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

    private void applyDefaultWorldSettings(World bukkitWorld) {
        if (difficulty != null) {
            bukkitWorld.setDifficulty(difficulty);
        }
        if (time != null) {
            bukkitWorld.setTime(time);
        }
        if (worldBorderSize != null) {
            bukkitWorld
                    .getWorldBorder()
                    .setSize(plugin.getConfigService()
                            .current()
                            .world()
                            .defaults()
                            .worldBorderSize());
        }
        bukkitWorld.setKeepSpawnInMemory(true);
        plugin.getConfigService()
                .current()
                .world()
                .defaults()
                .gameRules()
                .forEach(gameRule -> applyGameRule(bukkitWorld, gameRule));
    }

    private static <T> void applyGameRule(World world, GameRuleEntry<T> entry) {
        world.setGameRule(entry.rule(), entry.value());
    }

    private static void applyPostGenerationSettings(World bukkitWorld, BuildWorldType worldType) {
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
}
