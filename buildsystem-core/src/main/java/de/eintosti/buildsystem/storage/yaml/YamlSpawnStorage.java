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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlSpawnStorage {

    private static final String SPAWN_KEY = "spawn";

    private final YamlStore store;

    public YamlSpawnStorage(BuildSystemPlugin plugin) {
        this.store = new YamlStore(plugin.getDataFolder(), "spawn.yml", plugin.getLogger());
        store.locked(store::reload);
    }

    public @Nullable String getSpawn() {
        return store.config().getString(SPAWN_KEY);
    }

    /**
     * Writes the spawn, in the logout-location format, or removes it when {@code spawn} is {@code null}.
     *
     * @return Whether the file was written
     */
    public boolean saveSpawn(@Nullable String spawn) {
        return store.atomicSave(() -> store.config().set(SPAWN_KEY, spawn));
    }
}
