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

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityStoreContractTest;
import de.eintosti.buildsystem.storage.StorageType;
import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
class YamlEntityStoreContractTest extends EntityStoreContractTest {

    @Override
    protected PluginConfig.Storage.Database settings() {
        return settings(StorageType.YAML, "localhost", 0, "buildsystem", "", "", tablePrefix);
    }

    @Override
    protected List<String> loadOrder(List<String> saved) {
        return saved;
    }
}
