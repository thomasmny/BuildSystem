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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.storage.yaml.YamlStatusStorage;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns every {@link BuildWorldStatus}, seeding the built-in defaults on first run and persisting all administrator
 * changes. Deleting a custom status cascades: every world that used it is reset to the {@link #getDefaultStatus()
 * default} status and the id is removed from every category that grouped it. Built-in statuses are protected.
 */
@NullMarked
public class WorldStatusRegistryImpl implements WorldStatusRegistry {

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl categoryRegistry;
    private final YamlStatusStorage storage;
    private final Map<String, WorldStatusImpl> statuses = new LinkedHashMap<>();

    public WorldStatusRegistryImpl(BuildSystemPlugin plugin, NavigatorCategoryRegistryImpl categoryRegistry) {
        this.plugin = plugin;
        this.categoryRegistry = categoryRegistry;
        this.storage = new YamlStatusStorage(plugin);

        this.statuses.putAll(storage.load());
        if (this.statuses.isEmpty()) {
            seedDefaults();
            storage.saveAll(this.statuses.values());
        }
    }

    private void seedDefaults() {
        put("not_started", "Not Started", "&c", XMaterial.RED_DYE, 1, true, true, "in_progress");
        put("in_progress", "In Progress", "&6", XMaterial.ORANGE_DYE, 2, true, true, null);
        put("almost_finished", "Almost Finished", "&a", XMaterial.LIME_DYE, 3, true, true, null);
        put("finished", "Finished", "&2", XMaterial.GREEN_DYE, 4, true, true, null);
        put("archive", "Archive", "&3", XMaterial.CYAN_DYE, 5, false, true, null);
        put("hidden", "Hidden", "&7", XMaterial.BONE_MEAL, 6, true, false, null);
    }

    private void put(
            String id,
            String displayName,
            String color,
            XMaterial icon,
            int order,
            boolean buildingAllowed,
            boolean visibleInNavigator,
            @Nullable String progressesTo) {
        // One-time migration: a server upgrading from pre-4.0 carries the customised status name in the legacy
        // "status_<id>" message key (the message store never prunes user keys). When present, adopt it so the
        // server's renames/translations survive the move of status names from messages.yml into statuses.yml.
        String[] styledName = migrateLegacyName(id, displayName, color);
        this.statuses.put(
                id,
                WorldStatusImpl.builder(id)
                        .displayName(styledName[1])
                        .color(styledName[0])
                        .icon(icon)
                        .order(order)
                        .buildingAllowed(buildingAllowed)
                        .visibleInNavigator(visibleInNavigator)
                        .progressesTo(progressesTo)
                        .builtIn(true)
                        .build());
    }

    /**
     * Resolves the seeded name/colour for a built-in status, preferring the value a pre-4.0 server stored under the
     * legacy {@code status_<id>} message key. Returns {@code [color, displayName]}. The legacy value embeds the colour
     * in the text (e.g. {@code "&cNot Started"}); a leading legacy colour token is split off so the new model keeps
     * colour and name separate. Falls back to the supplied defaults when no legacy key is present.
     */
    private String[] migrateLegacyName(String id, String defaultName, String defaultColor) {
        Optional<String> legacy = plugin.getMessages().findRaw("status_" + id);
        if (legacy.isEmpty() || legacy.get().isBlank()) {
            return new String[] {defaultColor, defaultName};
        }

        String raw = legacy.get().strip();
        if (raw.startsWith("&#") && raw.length() >= 8) {
            return new String[] {raw.substring(0, 8), raw.substring(8).strip()};
        }
        if (raw.startsWith("&") && raw.length() >= 2) {
            return new String[] {raw.substring(0, 2), raw.substring(2).strip()};
        }
        return new String[] {defaultColor, raw};
    }

    @Override
    public Collection<BuildWorldStatus> getStatuses() {
        List<BuildWorldStatus> ordered = new ArrayList<>(this.statuses.values());
        ordered.sort(Comparator.comparingInt(BuildWorldStatus::getOrder));
        return Collections.unmodifiableList(ordered);
    }

    @Override
    public Optional<BuildWorldStatus> getStatus(String id) {
        return Optional.ofNullable(this.statuses.get(id));
    }

    @Override
    public BuildWorldStatus getDefaultStatus() {
        if (this.statuses.isEmpty()) {
            seedDefaults();
            storage.saveAll(this.statuses.values());
        }
        WorldStatusImpl notStarted = this.statuses.get(NOT_STARTED_ID);
        // Prefer the built-in fallback, but it may have been deleted by an admin; then the lowest-order status wins.
        return notStarted != null ? notStarted : getStatuses().iterator().next();
    }

    /**
     * Restores the six built-in statuses, discarding any customizations. Used by the setup menu's "reset to defaults"
     * control so an admin can always get back to a known-good state.
     */
    public void resetToDefaults() {
        this.statuses.clear();
        seedDefaults();
        storage.saveAll(this.statuses.values());
    }

    public WorldStatusImpl createStatus(String displayName) {
        String id = uniqueId(displayName);
        int order = this.statuses.values().stream()
                        .mapToInt(WorldStatusImpl::getOrder)
                        .max()
                        .orElse(0)
                + 1;
        WorldStatusImpl status = WorldStatusImpl.builder(id)
                .displayName(displayName)
                .order(order)
                .build();
        this.statuses.put(id, status);
        storage.save(status);
        categoryRegistry.addStatusToDefaultCategory(id);
        return status;
    }

    public void persist(WorldStatusImpl status) {
        storage.save(status);
    }

    /**
     * Lists the loaded worlds currently using the given status, for the deletion confirmation prompt.
     */
    public List<BuildWorld> worldsWithStatus(String id) {
        return plugin.getWorldService().getWorldStorage().getBuildWorlds().stream()
                .filter(world -> world.getData().getStatus().getId().equals(id))
                .toList();
    }

    /**
     * Deletes a status (built-in or custom), cascading every world that used it back to the
     * {@link #getDefaultStatus() default} and removing the id from every category that grouped it. The last remaining
     * status is never deleted, so a valid default always exists; an admin can restore the built-ins with
     * {@link #resetToDefaults()}.
     *
     * @return {@code true} if the status was deleted, {@code false} if it was unknown or the last remaining status
     */
    public boolean deleteStatus(String id) {
        if (!this.statuses.containsKey(id) || this.statuses.size() <= 1) {
            return false;
        }

        this.statuses.remove(id);
        BuildWorldStatus fallback = getDefaultStatus();
        for (BuildWorld world : worldsWithStatus(id)) {
            world.getData().setStatus(fallback);
            plugin.getWorldService().getWorldStorage().save(world);
        }

        clearDanglingProgression(id);
        categoryRegistry.removeStatusFromCategories(id);
        storage.delete(id);
        return true;
    }

    /**
     * Clears the {@code progressesTo} target on any sibling status that auto-advanced to the just-deleted status, so no
     * status is left pointing at an id that no longer resolves.
     */
    private void clearDanglingProgression(String deletedId) {
        for (WorldStatusImpl status : this.statuses.values()) {
            if (status.getProgressesTo().filter(deletedId::equals).isPresent()) {
                status.setProgressesTo(null);
                persist(status);
            }
        }
    }

    private String uniqueId(String displayName) {
        String base = displayName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "status";
        }
        String id = base;
        int suffix = 2;
        while (this.statuses.containsKey(id)) {
            id = base + "_" + suffix++;
        }
        return id;
    }
}
