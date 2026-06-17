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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Persists {@link NavigatorCategoryImpl navigator categories} to {@code categories.yml}. Each category is loaded in
 * isolation: a malformed entry is logged and skipped rather than aborting the whole load.
 */
@NullMarked
public class YamlCategoryStorage extends AbstractYamlStorage {

    private static final String CATEGORIES_KEY = "categories";

    private final BuildSystemPlugin plugin;

    public YamlCategoryStorage(BuildSystemPlugin plugin) {
        super(plugin, "categories.yml");
        this.plugin = plugin;
    }

    public Map<String, NavigatorCategoryImpl> load() {
        Map<String, NavigatorCategoryImpl> categories = new LinkedHashMap<>();
        FileConfiguration config = getFile();
        if (config == null) {
            return categories;
        }

        ConfigurationSection section = config.getConfigurationSection(CATEGORIES_KEY);
        if (section == null) {
            return categories;
        }

        for (String id : section.getKeys(false)) {
            try {
                categories.put(id, parseCategory(section.getConfigurationSection(id), id));
            } catch (Exception e) {
                plugin.getLogger().warning("Skipping navigator category \"" + id + "\": " + e.getMessage());
            }
        }
        return categories;
    }

    private NavigatorCategoryImpl parseCategory(ConfigurationSection section, String id) {
        EnumSet<Visibility> visibilities = EnumSet.noneOf(Visibility.class);
        for (String raw : section.getStringList("visibilities")) {
            try {
                visibilities.add(Visibility.valueOf(raw.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Skip unknown visibility names rather than failing the whole category.
            }
        }
        if (visibilities.isEmpty()) {
            visibilities.add(Visibility.EVERYONE);
        }

        XMaterial icon =
                XMaterial.matchXMaterial(section.getString("icon", "OAK_SIGN")).orElse(XMaterial.OAK_SIGN);
        List<String> statusIds = section.getStringList("statuses");
        return NavigatorCategoryImpl.builder(id)
                .displayName(section.getString("display-name", id))
                .color(section.getString("color", "&7"))
                .icon(icon)
                .iconSkullTexture(section.getString("icon-skull-texture"))
                .visibilities(visibilities)
                .shownInNavigator(section.getBoolean("shown-in-navigator", true))
                .navigatorSlot(section.getInt("navigator-slot", 0))
                .builtIn(section.getBoolean("built-in", false))
                .statusIds(statusIds)
                .build();
    }

    public void saveAll(Collection<NavigatorCategoryImpl> categories) {
        FileConfiguration config = getFile();
        if (config == null) {
            return;
        }
        config.set(CATEGORIES_KEY, null);
        for (NavigatorCategoryImpl category : categories) {
            write(config, category);
        }
        saveFile();
    }

    public void save(NavigatorCategoryImpl category) {
        FileConfiguration config = getFile();
        if (config == null) {
            return;
        }
        write(config, category);
        saveFile();
    }

    public void delete(String id) {
        FileConfiguration config = getFile();
        if (config == null) {
            return;
        }
        config.set(CATEGORIES_KEY + "." + id, null);
        saveFile();
    }

    private void write(FileConfiguration config, NavigatorCategoryImpl category) {
        String path = CATEGORIES_KEY + "." + category.getId();
        config.set(path + ".display-name", category.getDisplayName());
        config.set(path + ".color", category.getColor());
        config.set(path + ".icon", category.getIcon().name());
        config.set(path + ".icon-skull-texture", category.getIconSkullTexture());
        config.set(
                path + ".visibilities",
                category.getVisibilities().stream().map(Visibility::name).toList());
        config.set(path + ".shown-in-navigator", category.isShownInNavigator());
        config.set(path + ".navigator-slot", category.getNavigatorSlot());
        config.set(path + ".built-in", category.isBuiltIn());
        config.set(path + ".statuses", category.getStatusIds());
    }
}
