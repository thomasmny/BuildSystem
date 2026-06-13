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
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import java.util.Map;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * State and helpers shared by {@link WorldBuilderImpl} (generating a fresh world) and {@link WorldImporterImpl}
 * (adopting an existing directory). Each subclass exposes only the fluent setters its public API interface declares.
 */
@NullMarked
abstract class AbstractWorldCreator {

    protected final BuildSystemPlugin plugin;
    protected final WorldStorageImpl worldStorage;
    protected final String worldName;

    protected BuildWorldType worldType;
    protected @Nullable Builder creator;
    protected boolean privateWorld = false;
    protected @Nullable CustomGenerator customGenerator = null;
    protected long creationDate = System.currentTimeMillis();
    protected @Nullable Folder folder;

    protected @Nullable Difficulty difficulty;
    protected @Nullable Integer time;
    protected @Nullable Integer worldBorderSize;

    protected @Nullable Player audience;

    protected @Nullable BuildWorld buildWorld;

    protected AbstractWorldCreator(BuildSystemPlugin plugin, String worldName, BuildWorldType worldType) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.worldName = worldName;
        this.worldType = worldType;
        this.difficulty = plugin.getConfigService().current().world().defaults().difficulty();
        this.time =
                plugin.getConfigService().current().world().defaults().time().noon();
        this.worldBorderSize =
                plugin.getConfigService().current().world().defaults().worldBorderSize();
    }

    protected BuildWorld createAndRegisterBuildWorld() {
        BuildWorldImpl bw = new BuildWorldImpl(
                plugin, worldName, creator, worldType, creationDate, privateWorld, customGenerator, folder);

        if (folder != null) {
            folder.addWorld(bw);
        }

        bw.getData().setLastLoaded(System.currentTimeMillis());
        worldStorage.addBuildWorld(bw);
        return bw;
    }

    protected @Nullable World generateBukkitWorld(boolean checkVersion) {
        if (buildWorld == null) {
            throw new IllegalStateException("BuildWorld must be set before generating the Bukkit world.");
        }

        return new BukkitWorldFactory(plugin, worldName, worldType, customGenerator, difficulty, time, worldBorderSize)
                .generate(
                        checkVersion ? BukkitWorldFactory.VersionCheck.REQUIRED : BukkitWorldFactory.VersionCheck.SKIP);
    }

    @SafeVarargs
    protected final void notifyAudience(String key, Map.Entry<String, Object>... placeholders) {
        if (audience != null) {
            plugin.getMessages().sendMessage(audience, key, placeholders);
        }
    }
}
