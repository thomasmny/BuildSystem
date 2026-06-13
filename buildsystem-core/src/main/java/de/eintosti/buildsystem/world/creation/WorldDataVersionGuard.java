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

import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.io.CompressionType;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldDataVersionGuard {

    private static final String LEVEL_DAT_FILE_NAME = "level.dat";

    private final Logger logger;
    private final String worldName;

    public WorldDataVersionGuard(Logger logger, String worldName) {
        this.logger = logger;
        this.worldName = worldName;
    }

    @SuppressWarnings("deprecation")
    public int getServerDataVersion() {
        return Bukkit.getServer().getUnsafe().getDataVersion();
    }

    public boolean isDataVersionTooHigh() {
        if (Boolean.getBoolean("Paper.ignoreWorldDataVersion")) {
            return false;
        }
        int worldVersion = parseDataVersion();
        return worldVersion > getServerDataVersion();
    }

    public int parseDataVersion() {
        File levelFile = new File(new File(Bukkit.getWorldContainer(), worldName), LEVEL_DAT_FILE_NAME);
        if (!levelFile.exists()) {
            return -1;
        }

        try {
            CompoundTag level = new Nbt().fromFile(levelFile);
            CompoundTag data = level.get("Data");
            IntTag dataVersion = data.getInt("DataVersion");
            return dataVersion != null ? dataVersion.getValue() : -1;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to parse level.dat for world " + worldName, e);
            return -1;
        }
    }

    public void updateWorldDataVersion() {
        File levelFile = new File(new File(Bukkit.getWorldContainer(), worldName), LEVEL_DAT_FILE_NAME);
        if (!levelFile.exists()) {
            return;
        }

        try {
            Nbt nbt = new Nbt();
            CompoundTag level = nbt.fromFile(levelFile);
            CompoundTag data = level.get("Data");
            IntTag dataVersionTag = data.getInt("DataVersion");
            if (dataVersionTag == null) {
                return;
            }

            int worldVersion = dataVersionTag.getValue();
            int serverVersion = getServerDataVersion();
            if (worldVersion < serverVersion) {
                dataVersionTag.setValue(serverVersion);
                nbt.toFile(level, levelFile, CompressionType.GZIP);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to update level.dat for world " + worldName, e);
        }
    }
}
