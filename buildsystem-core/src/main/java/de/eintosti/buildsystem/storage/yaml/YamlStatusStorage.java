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
import de.eintosti.buildsystem.world.data.WorldStatusImpl;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Persists {@link WorldStatusImpl statuses} to {@code statuses.yml}. Each status is loaded in isolation: a malformed
 * entry is logged and skipped rather than aborting the whole load.
 */
@NullMarked
public class YamlStatusStorage extends AbstractYamlStorage {

    private static final String STATUSES_KEY = "statuses";

    private final BuildSystemPlugin plugin;

    public YamlStatusStorage(BuildSystemPlugin plugin) {
        super(plugin, "statuses.yml");
        this.plugin = plugin;
    }

    public Map<String, WorldStatusImpl> load() {
        Map<String, WorldStatusImpl> statuses = new LinkedHashMap<>();
        FileConfiguration config = getFile();
        if (config == null) {
            return statuses;
        }

        ConfigurationSection section = config.getConfigurationSection(STATUSES_KEY);
        if (section == null) {
            return statuses;
        }

        for (String id : section.getKeys(false)) {
            try {
                statuses.put(id, parseStatus(section.getConfigurationSection(id), id));
            } catch (Exception e) {
                plugin.getLogger().warning("Skipping status \"" + id + "\": " + e.getMessage());
            }
        }
        return statuses;
    }

    private WorldStatusImpl parseStatus(ConfigurationSection section, String id) {
        XMaterial icon =
                XMaterial.matchXMaterial(section.getString("icon", "WHITE_DYE")).orElse(XMaterial.WHITE_DYE);
        String progressesTo = section.getString("progresses-to");
        return WorldStatusImpl.builder(id)
                .displayName(section.getString("display-name", id))
                .color(section.getString("color", "&7"))
                .icon(icon)
                .order(section.getInt("order", 0))
                .buildingAllowed(section.getBoolean("building-allowed", true))
                .progressesTo(progressesTo != null && !progressesTo.isBlank() ? progressesTo : null)
                .builtIn(section.getBoolean("built-in", false))
                // A pre-layout statuses.yml has neither key: the status is left unplaced (slot -1) so the registry can
                // migrate it into the grid by order on load.
                .statusSlot(section.getInt("status-slot", -1))
                .shownInStatusMenu(section.getBoolean("shown-in-status-menu", true))
                .build();
    }

    public void saveAll(Collection<WorldStatusImpl> statuses) {
        FileConfiguration config = getFile();
        if (config == null) {
            return;
        }
        config.set(STATUSES_KEY, null);
        for (WorldStatusImpl status : statuses) {
            write(config, status);
        }
        saveFile();
    }

    public void save(WorldStatusImpl status) {
        FileConfiguration config = getFile();
        if (config == null) {
            return;
        }
        write(config, status);
        saveFile();
    }

    public void delete(String id) {
        FileConfiguration config = getFile();
        if (config == null) {
            return;
        }
        config.set(STATUSES_KEY + "." + id, null);
        saveFile();
    }

    private void write(FileConfiguration config, WorldStatusImpl status) {
        String path = STATUSES_KEY + "." + status.getId();
        config.set(path + ".display-name", status.getDisplayName());
        config.set(path + ".color", status.getColor());
        config.set(path + ".icon", status.getIcon().name());
        config.set(path + ".order", status.getOrder());
        config.set(path + ".building-allowed", status.isBuildingAllowed());
        config.set(path + ".progresses-to", status.getProgressesTo().orElse(null));
        config.set(path + ".built-in", status.isBuiltIn());
        config.set(path + ".status-slot", status.getStatusSlot());
        config.set(path + ".shown-in-status-menu", status.isShownInStatusMenu());
    }
}
