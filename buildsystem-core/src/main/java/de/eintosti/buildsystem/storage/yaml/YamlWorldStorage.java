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
package de.eintosti.buildsystem.storage.yaml;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.codec.WorldCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class YamlWorldStorage extends WorldStorageImpl {

    private static final String WORLDS_KEY = "worlds";

    private final YamlStore store;
    private final FileConfiguration config;
    private final WorldCodec codec;

    public YamlWorldStorage(BuildSystemPlugin plugin) {
        super(plugin.getLogger());
        this.store = new YamlStore(plugin.getDataFolder(), "worlds.yml", plugin.getLogger());
        this.config = store.config();
        this.codec = new WorldCodec(plugin);
    }

    @Override
    public CompletableFuture<Void> save(BuildWorld buildWorld) {
        return CompletableFuture.runAsync(() -> store.atomicSave(
                () -> config.set(WORLDS_KEY + "." + codec.key(buildWorld), codec.serialize(buildWorld))));
    }

    @Override
    public CompletableFuture<Void> save(Collection<BuildWorld> buildWorlds) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() -> buildWorlds.forEach(
                buildWorld -> config.set(WORLDS_KEY + "." + codec.key(buildWorld), codec.serialize(buildWorld)))));
    }

    @Override
    public CompletableFuture<Collection<BuildWorld>> load() {
        return CompletableFuture.supplyAsync(() -> store.locked(() -> {
            Collection<BuildWorld> worlds = new ArrayList<>();
            for (String worldName : loadWorldKeys()) {
                try {
                    ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY + "." + worldName);
                    if (section == null) {
                        continue;
                    }
                    worlds.add(codec.deserialize(worldName, section));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Skipping world \"" + worldName + "\": could not be loaded", e);
                }
            }
            return worlds;
        }));
    }

    private Set<String> loadWorldKeys() {
        if (!store.reload()) {
            return Set.of();
        }

        ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    @Override
    public CompletableFuture<Void> delete(BuildWorld buildWorld) {
        return delete(buildWorld.getName());
    }

    @Override
    public CompletableFuture<Void> delete(String worldKey) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() -> config.set(WORLDS_KEY + "." + worldKey, null)));
    }
}
