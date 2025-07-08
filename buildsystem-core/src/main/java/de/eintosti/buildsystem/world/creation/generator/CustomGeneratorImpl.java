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
package de.eintosti.buildsystem.world.creation.generator;

import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a custom chunk generator identified by its plugin name and generator name. This record provides immutable storage for these identifiers and an optional reference to
 * the actual {@link ChunkGenerator} instance if it could be loaded.
 * <p>
 * The {@code pluginName} typically refers to the name of the plugin that provides the custom chunk generator. The {@code chunkGeneratorName} is the specific name or ID of the
 * generator within that plugin.
 *
 * @param pluginName         The name of the plugin providing the chunk generator (e.g., "MyPlugin")
 * @param chunkGeneratorName The specific name of the chunk generator within the plugin (e.g., "MyGenerator" or an empty string for default)
 * @param chunkGenerator     An optional reference to the actual Bukkit {@link ChunkGenerator} instance, or {@code null} if it could not be retrieved or is not needed
 */
@NullMarked
public record CustomGeneratorImpl(String pluginName, String chunkGeneratorName, @Nullable ChunkGenerator chunkGenerator) implements CustomGenerator {

    /**
     * Attempts to create a {@link CustomGeneratorImpl} instance by parsing an identifier string and loading the corresponding {@link ChunkGenerator}.
     * <p>
     * The {@code identifier} string is expected to be in one of two formats:
     * <ul>
     * <li>{@code "pluginName:chunkGeneratorName"} (e.g., "MyPlugin:MyGenerator")</li>
     * <li>{@code "pluginName"} (e.g., "MyPlugin") - In this case, {@code chunkGeneratorName} is inferred to be the same as {@code pluginName}, which is a common convention for single-plugin generators.</li>
     * </ul>
     * The method attempts to find the specified plugin and then retrieve its default world generator.
     *
     * @param identifier The string identifier of the custom generator (e.g., "MyPlugin:MyGenerator" or "MyPlugin")
     * @param worldName  The name of the world for which the generator is being loaded
     * @return A {@link CustomGeneratorImpl} instance if the plugin is found and the generator information is parsed; {@code null} if the plugin specified in the identifier does
     * not exist or if the identifier format is unexpectedly empty
     */
    @Nullable
    public static CustomGeneratorImpl of(String identifier, String worldName) {
        String[] generatorInfo = identifier.split(":");
        if (generatorInfo.length == 1) {
            generatorInfo = new String[]{generatorInfo[0], generatorInfo[0]};
        }

        String pluginName = generatorInfo[0];
        String chunkGeneratorName = generatorInfo[1];

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return null;
        }

        return new CustomGeneratorImpl(pluginName, chunkGeneratorName, plugin.getDefaultWorldGenerator(worldName, chunkGeneratorName));
    }

    @Override
    public String toString() {
        return "%s:%s".formatted(pluginName(), chunkGeneratorName());
    }
}