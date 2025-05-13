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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.builder.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Manages world data updates and persistence.
 */
public class WorldDataManager {

    private final BuildSystem plugin;

    public WorldDataManager(BuildSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates the data associated with a build world. This method creates a new WorldData instance with updated values and persists the changes.
     *
     * @param buildWorld The build world to update
     * @param worldData  The new world data to copy values from
     */
    public void updateWorldData(BuildWorld buildWorld, WorldData worldData) {
        buildWorld.setWorldData(worldData);
        plugin.getWorldService().save();
    }

    /**
     * Updates a specific field in the world data.
     *
     * @param buildWorld The build world to update
     * @param updater    A function that creates a new WorldData with the updated field
     */
    public void updateField(BuildWorld buildWorld, java.util.function.Function<WorldData, WorldData> updater) {
        WorldData currentData = buildWorld.getData();
        WorldData updatedData = updater.apply(currentData);
        buildWorld.setWorldData(updatedData);
        plugin.getWorldService().save();
    }

    /**
     * Updates the custom spawn location.
     *
     * @param buildWorld The build world to update
     * @param location   The new spawn location
     */
    public void updateCustomSpawn(BuildWorld buildWorld, Location location) {
        updateField(buildWorld, data -> data.withCustomSpawn(location));
    }

    /**
     * Updates the world's permission.
     *
     * @param buildWorld The build world to update
     * @param permission The new permission
     */
    public void updatePermission(BuildWorld buildWorld, String permission) {
        updateField(buildWorld, data -> data.withPermission(permission));
    }

    public WorldData parseWorldData(ConfigurationSection section, String worldName) {
        if (section == null) {
            return WorldData.createDefault(worldName);
        }

        Location customSpawn = null;
        String spawnString = section.getString("spawn");
        if (spawnString != null) {
            String[] spawnParts = spawnString.split(";");
            if (spawnParts.length == 5) {
                customSpawn = new Location(
                        plugin.getServer().getWorld(worldName),
                        Double.parseDouble(spawnParts[0]),
                        Double.parseDouble(spawnParts[1]),
                        Double.parseDouble(spawnParts[2]),
                        Float.parseFloat(spawnParts[3]),
                        Float.parseFloat(spawnParts[4])
                );
            }
        }

        String permission = section.getString("permission", "-");
        String project = section.getString("project", "-");
        Difficulty difficulty = Difficulty.valueOf(
                section.getString("difficulty", "NORMAL").toUpperCase(Locale.ROOT)
        );
        XMaterial material = XMaterial.matchXMaterial(
                section.getString("material", "GRASS_BLOCK")
        ).orElse(XMaterial.GRASS_BLOCK);
        WorldStatus status = WorldStatus.valueOf(
                section.getString("status", "NOT_STARTED")
        );

        return new WorldData(
                customSpawn,
                permission,
                project,
                difficulty,
                material,
                status,
                section.getBoolean("block-breaking", true),
                section.getBoolean("block-interactions", true),
                section.getBoolean("block-placement", true),
                section.getBoolean("builders-enabled", false),
                section.getBoolean("explosions", true),
                section.getBoolean("mob-ai", true),
                section.getBoolean("physics", true),
                section.getBoolean("private", false),
                section.getLong("last-edited", -1),
                section.getLong("last-loaded", -1),
                section.getLong("last-unloaded", -1),
                worldName
        );
    }

    public Builder parseCreator(ConfigurationSection configuration, String worldName) {
        final String creator = configuration.getString("worlds." + worldName + ".creator");
        final String oldCreatorIdPath = "worlds." + worldName + ".creator-id";
        final String oldCreatorId = configuration.isString(oldCreatorIdPath)
                ? configuration.getString(oldCreatorIdPath)
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

    public List<Builder> parseBuilders(ConfigurationSection configuration, String worldName) {
        List<Builder> builders = new ArrayList<>();

        if (configuration.isString("worlds." + worldName + ".builders")) {
            String buildersString = configuration.getString("worlds." + worldName + ".builders");
            if (buildersString != null && !buildersString.isEmpty()) {
                String[] splitBuilders = buildersString.split(";");
                for (String builder : splitBuilders) {
                    builders.add(Builder.deserialize(builder));
                }
            }
        }

        return builders;
    }
} 