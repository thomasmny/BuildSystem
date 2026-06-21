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
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base for the simple (non-cache-backed) storages. Holds a {@link YamlStore} for the file plumbing and exposes it
 * through the {@code loadFile}/{@code saveFile}/{@code getFile} API its subclasses use, so all storages — simple and
 * cache-backed alike — share one plumbing implementation.
 */
@NullMarked
public abstract class AbstractYamlStorage {

    private final YamlStore store;

    public AbstractYamlStorage(BuildSystemPlugin plugin, String fileName) {
        this.store = new YamlStore(plugin.getDataFolder(), fileName, plugin.getLogger());
        loadFile();
    }

    public void loadFile() {
        store.reload();
    }

    public void saveFile() {
        store.save();
    }

    public @Nullable FileConfiguration getFile() {
        return store.config();
    }
}
