/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.BuildSystem;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

public class SetupConfig extends ConfigurationFile {

    public SetupConfig(BuildSystem plugin) {
        super(plugin, "setup.yml");
    }

    public <T extends Enum<?>> void saveIcon(String key, T type, XMaterial material) {
        getFile().set("setup." + key + "." + type.name().toLowerCase(Locale.ROOT), material.name());
        saveFile();
    }

    @Nullable
    public <T> Map<T, XMaterial> loadIcons(String key, Function<String, T> mapper) {
        FileConfiguration configuration = getFile();
        if (configuration == null) {
            return null;
        }

        ConfigurationSection configurationSection = configuration.getConfigurationSection("setup." + key);
        if (configurationSection == null) {
            return null;
        }

        Set<String> types = configurationSection.getKeys(false);
        if (types.isEmpty()) {
            return null;
        }

        Map<T, XMaterial> icons = new HashMap<>();

        for (String type : types) {
            String materialString = configuration.getString("setup." + key + "." + type);
            if (materialString == null) {
                continue;
            }

            T mappedKey = mapper.apply(type);
            XMaterial.matchXMaterial(materialString).ifPresent(material -> icons.put(mappedKey, material));
        }

        return icons;
    }
}