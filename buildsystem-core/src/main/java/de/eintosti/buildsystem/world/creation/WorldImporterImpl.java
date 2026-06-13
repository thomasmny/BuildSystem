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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.WorldImporter;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import java.io.File;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldImporterImpl extends AbstractWorldCreator implements WorldImporter {

    private final WorldDataVersionGuard versionGuard;

    public WorldImporterImpl(BuildSystemPlugin plugin, String worldName) {
        super(plugin, worldName, BuildWorldType.IMPORTED);
        this.versionGuard = new WorldDataVersionGuard(plugin.getLogger(), worldName);
        this.creationDate = FileUtils.getDirectoryCreation(new File(Bukkit.getWorldContainer(), worldName));
    }

    @Override
    @Contract("_ -> this")
    public WorldImporterImpl creator(@Nullable Builder creator) {
        this.creator = creator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldImporterImpl type(BuildWorldType type) {
        this.worldType = type;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldImporterImpl customGenerator(@Nullable CustomGenerator customGenerator) {
        this.customGenerator = customGenerator;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldImporterImpl folder(@Nullable Folder folder) {
        this.folder = folder;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldImporterImpl privateWorld(boolean privateWorld) {
        this.privateWorld = privateWorld;
        return this;
    }

    @Override
    @Contract("_ -> this")
    public WorldImporterImpl notify(@Nullable Player audience) {
        this.audience = audience;
        return this;
    }

    @Override
    protected boolean isImport() {
        return true;
    }

    @Override
    public @Nullable BuildWorld build() {
        if (isDataVersionTooHigh()) {
            notifyAudience("worlds_import_newer_version", Map.entry("%world%", worldName));
            return null;
        }

        if (isCreationCancelled()) {
            return null;
        }

        if (customGenerator == null) {
            // Imported worlds default to a void generator so unexplored chunks are not filled with terrain.
            customGenerator = new CustomGeneratorImpl("BuildSystem", "void", null);
        }

        buildWorld = createAndRegisterBuildWorld();
        generateBukkitWorld(true);
        return buildWorld;
    }

    /**
     * Gets whether the world directory was last saved by a newer Minecraft version than the server is running, in
     * which case importing it would corrupt it.
     */
    public boolean isDataVersionTooHigh() {
        return versionGuard.isDataVersionTooHigh();
    }
}
