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
public class YamlSpawnStorage extends AbstractYamlStorage {

    private static final String SPAWN_KEY = "spawn";

    public YamlSpawnStorage(BuildSystemPlugin plugin) {
        super(plugin, "spawn.yml");
    }

    public @Nullable String getSpawn() {
        return getFile().getString(SPAWN_KEY);
    }

    /**
     * Writes the spawn, in the logout-location format, or removes it when {@code spawn} is {@code null}.
     *
     * @return Whether the file was written
     */
    public boolean saveSpawn(@Nullable String spawn) {
        getFile().set(SPAWN_KEY, spawn);
        return saveFile();
    }
}
