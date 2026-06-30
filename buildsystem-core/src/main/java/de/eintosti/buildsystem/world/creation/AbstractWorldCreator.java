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

import de.eintosti.buildsystem.api.event.world.BuildWorldCreateEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldPostCreateEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import java.util.Map;
import org.bukkit.Bukkit;
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

    protected final WorldContext context;
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
    protected @Nullable Long seed;

    protected @Nullable Player audience;

    protected @Nullable BuildWorld buildWorld;

    protected AbstractWorldCreator(
            WorldContext context, WorldStorageImpl worldStorage, String worldName, BuildWorldType worldType) {
        this.context = context;
        this.worldStorage = worldStorage;
        this.worldName = worldName;
        this.worldType = worldType;
        this.difficulty = context.configService().current().world().defaults().difficulty();
        this.time = context.configService().current().world().defaults().time().noon();
        this.worldBorderSize =
                context.configService().current().world().defaults().worldBorderSize();
    }

    /**
     * Whether this creator imports an existing directory ({@code true}) or generates a new world ({@code false}).
     */
    protected abstract boolean isImport();

    /**
     * Fires the cancellable {@link BuildWorldCreateEvent}. Call before any world directory is created or copied.
     *
     * @return {@code true} if a listener cancelled creation (callers must abort and return {@code null})
     */
    protected boolean isCreationCancelled() {
        BuildWorldCreateEvent event = new BuildWorldCreateEvent(worldName, worldType, creator, isImport());
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    protected BuildWorld createAndRegisterBuildWorld() {
        BuildWorldImpl newBuildWorld = new BuildWorldImpl(
                context, worldName, creator, worldType, creationDate, privateWorld, customGenerator, folder);

        if (folder != null) {
            folder.addWorld(newBuildWorld);
        }

        newBuildWorld.getData().set(WorldDataKey.LAST_LOADED, System.currentTimeMillis());
        worldStorage.addBuildWorld(newBuildWorld);
        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostCreateEvent(newBuildWorld, isImport()));
        return newBuildWorld;
    }

    protected @Nullable World generateBukkitWorld(boolean checkVersion) {
        if (buildWorld == null) {
            throw new IllegalStateException("BuildWorld must be set before generating the Bukkit world.");
        }

        return new BukkitWorldFactory(
                        context.configService(),
                        context.logger(),
                        worldName,
                        worldType,
                        customGenerator,
                        difficulty,
                        time,
                        worldBorderSize,
                        seed)
                .generate(
                        checkVersion ? BukkitWorldFactory.VersionCheck.REQUIRED : BukkitWorldFactory.VersionCheck.SKIP);
    }

    @SafeVarargs
    protected final void notifyAudience(String key, Map.Entry<String, Object>... placeholders) {
        if (audience != null) {
            context.messages().sendMessage(audience, key, placeholders);
        }
    }
}
