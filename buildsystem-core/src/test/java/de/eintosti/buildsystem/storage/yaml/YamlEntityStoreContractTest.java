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

import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.EntityStoreContractTest;
import java.io.File;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class YamlEntityStoreContractTest extends EntityStoreContractTest {

    @TempDir
    File dataFolder;

    @Override
    protected EntityStore store(String name) {
        return yamlStore(dataFolder, name);
    }

    @Override
    protected List<String> loadOrder(List<String> saved) {
        return saved;
    }
}
