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
package de.eintosti.buildsystem.storage;

import de.eintosti.buildsystem.storage.yaml.YamlEntityStore;
import java.io.File;
import java.util.Locale;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * The kinds of entity a {@link StorageBackend} keeps. Table and collection names come only from here, never from the
 * config.
 */
@NullMarked
public enum EntityKind {
    WORLDS,
    FOLDERS,
    PLAYERS;

    /**
     * {@return the collection's name, the root key of its YAML file and, after the prefix, its table name}
     */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public YamlEntityStore yaml(File dataFolder, Logger logger) {
        return switch (this) {
            case WORLDS -> YamlEntityStore.worlds(dataFolder, logger);
            case FOLDERS -> YamlEntityStore.folders(dataFolder, logger);
            case PLAYERS -> YamlEntityStore.players(dataFolder, logger);
        };
    }
}
