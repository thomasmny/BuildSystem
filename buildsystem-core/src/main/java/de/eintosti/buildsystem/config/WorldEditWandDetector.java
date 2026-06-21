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
package de.eintosti.buildsystem.config;

import com.cryptomorin.xseries.XMaterial;
import java.io.File;
import java.util.Locale;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the WorldEdit wand material by reading WorldEdit's (or FastAsyncWorldEdit's) own config file, so the plugin
 * can recognise that item inside protected worlds. Kept out of {@link ConfigService}'s parsing so config parsing stays
 * a pure, side-effect-free transform of the {@code FileConfiguration} — the filesystem lookup lives here and is easy to
 * stub in tests by constructing the detector with a controlled directory (or {@code null} to skip detection).
 */
@NullMarked
public final class WorldEditWandDetector {

    /** Used when no WorldEdit install is found or its wand cannot be resolved. */
    public static final XMaterial DEFAULT_WAND = XMaterial.WOODEN_AXE;

    private final @Nullable File pluginsDir;

    /**
     * @param pluginsDir The server's plugins directory (the parent of this plugin's data folder), or {@code null} to
     *     skip detection and always return {@link #DEFAULT_WAND}
     */
    public WorldEditWandDetector(@Nullable File pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    /**
     * {@return the configured WorldEdit wand, or {@link #DEFAULT_WAND} when WorldEdit is absent or its wand cannot be
     * resolved}
     */
    public XMaterial detect() {
        File configFile = locateConfig();
        if (configFile == null) {
            return DEFAULT_WAND;
        }

        YamlConfiguration weConfig = YamlConfiguration.loadConfiguration(configFile);
        String wand = weConfig.getString("wand-item");
        if (wand == null) {
            return DEFAULT_WAND;
        }

        String namespace = "minecraft:";
        if (wand.toLowerCase(Locale.ROOT).startsWith(namespace)) {
            wand = wand.substring(namespace.length());
        }
        return XMaterial.matchXMaterial(wand).orElse(DEFAULT_WAND);
    }

    /**
     * FastAsyncWorldEdit takes precedence over WorldEdit when both are installed, matching the prior behaviour.
     */
    private @Nullable File locateConfig() {
        if (pluginsDir == null) {
            return null;
        }

        File faweConfig = new File(pluginsDir + File.separator + "FastAsyncWorldEdit", "worldedit-config.yml");
        if (faweConfig.exists()) {
            return faweConfig;
        }

        File weConfig = new File(pluginsDir + File.separator + "WorldEdit", "config.yml");
        if (weConfig.exists()) {
            return weConfig;
        }

        return null;
    }
}
