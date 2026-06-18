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
package de.eintosti.buildsystem.world.display;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.storage.yaml.YamlCategoryStorage;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns every {@link NavigatorCategory}, seeding the built-in defaults on first run and persisting all administrator
 * changes. The category a world is displayed in is resolved by matching both the category's
 * {@link NavigatorCategory#getVisibilities() visibilities} and its statuses against the world. Any category may be
 * deleted except the last remaining one (so a default always exists); {@link #resetToDefaults()} restores the
 * built-ins. Deleting a category never orphans a status because statuses are shared, not owned.
 */
@NullMarked
public class NavigatorCategoryRegistryImpl implements NavigatorCategoryRegistry {

    /** Slot the settings button occupies in a freshly seeded navigator (matches the historical fixed slot). */
    public static final int DEFAULT_SETTINGS_SLOT = 15;

    private final BuildSystemPlugin plugin;
    private final YamlCategoryStorage storage;
    private final Map<String, NavigatorCategoryImpl> categories = new LinkedHashMap<>();
    private int settingsSlot;

    public NavigatorCategoryRegistryImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.storage = new YamlCategoryStorage(plugin);

        this.categories.putAll(storage.load());
        if (this.categories.isEmpty()) {
            seedDefaults();
            storage.saveAll(this.categories.values());
        }
        this.settingsSlot = storage.loadSettingsSlot(DEFAULT_SETTINGS_SLOT);
    }

    /**
     * Gets the navigator slot the settings button occupies. Both the live navigator and the layout editor read this so
     * the editor mirrors the real navigator exactly.
     *
     * @return The settings slot
     */
    public int getSettingsSlot() {
        return settingsSlot;
    }

    /**
     * Sets and persists the navigator slot the settings button occupies.
     *
     * @param slot The new settings slot
     */
    public void setSettingsSlot(int slot) {
        this.settingsSlot = slot;
        storage.saveSettingsSlot(slot);
    }

    private void seedDefaults() {
        this.categories.putAll(buildDefaults());
    }

    /**
     * {@return a fresh map of the built-in categories in their default state} Built on demand without touching the live
     * registry, so it can back both first-run seeding and the navigator-layout reset.
     */
    private Map<String, NavigatorCategoryImpl> buildDefaults() {
        Map<String, NavigatorCategoryImpl> defaults = new LinkedHashMap<>();
        List<String> activeStatuses = List.of("not_started", "in_progress", "almost_finished", "finished");
        // Built-in categories keep the pre-4.0 navigator icons: textured player-head skulls. The private category
        // defaults to the viewing player's own head ({@code %viewer%}).
        defaults.put(
                PUBLIC_ID,
                NavigatorCategoryImpl.builder(PUBLIC_ID)
                        .displayName("Worlds")
                        .color("&b")
                        .icon(XMaterial.PLAYER_HEAD)
                        .iconSkullTexture(SkullTextures.WORLD_NAVIGATOR)
                        .visibilities(EnumSet.of(Visibility.EVERYONE))
                        .navigatorSlot(11)
                        .builtIn(true)
                        .statusIds(activeStatuses)
                        .build());
        defaults.put(
                ARCHIVE_ID,
                NavigatorCategoryImpl.builder(ARCHIVE_ID)
                        .displayName("Archive")
                        .color("&3")
                        .icon(XMaterial.PLAYER_HEAD)
                        .iconSkullTexture(SkullTextures.WORLD_ARCHIVE)
                        .visibilities(EnumSet.of(Visibility.EVERYONE, Visibility.ADDED_PLAYERS))
                        .navigatorSlot(12)
                        .builtIn(true)
                        .statusIds(List.of("archive"))
                        .build());
        defaults.put(
                PRIVATE_ID,
                NavigatorCategoryImpl.builder(PRIVATE_ID)
                        .displayName("Private")
                        .color("&a")
                        .icon(XMaterial.PLAYER_HEAD)
                        .iconSkullTexture(ItemBuilder.VIEWER_HEAD)
                        .visibilities(EnumSet.of(Visibility.ADDED_PLAYERS))
                        .navigatorSlot(13)
                        .builtIn(true)
                        .statusIds(activeStatuses)
                        .build());
        return defaults;
    }

    private void put(NavigatorCategoryImpl category) {
        this.categories.put(category.getId(), category);
    }

    @Override
    public Collection<NavigatorCategory> getCategories() {
        List<NavigatorCategory> ordered = new ArrayList<>(this.categories.values());
        ordered.sort(Comparator.comparingInt(NavigatorCategory::getNavigatorSlot));
        return Collections.unmodifiableList(ordered);
    }

    @Override
    public Optional<NavigatorCategory> getCategory(@Nullable String id) {
        return Optional.ofNullable(this.categories.get(id));
    }

    @Override
    public NavigatorCategory getCategoryForWorld(BuildWorld world) {
        WorldData data = world.getData();
        String statusId = data.getStatus().getId();
        for (NavigatorCategory category : getCategories()) {
            if (category.getVisibilities().contains(data.getVisibility())
                    && category.getStatusIds().contains(statusId)) {
                return category;
            }
        }
        return getDefaultCategory();
    }

    @Override
    public NavigatorCategory getDefaultCategory() {
        if (this.categories.isEmpty()) {
            seedDefaults();
            storage.saveAll(this.categories.values());
        }
        NavigatorCategoryImpl publicCategory = this.categories.get(PUBLIC_ID);
        // Prefer the built-in public category, but it may have been deleted; then the lowest-slot category wins.
        return publicCategory != null
                ? publicCategory
                : getCategories().iterator().next();
    }

    /**
     * Restores the three built-in categories, discarding any customizations. Used by the setup menu's "reset to
     * defaults" control so an admin can always get back to a known-good state.
     */
    public void resetToDefaults() {
        this.categories.clear();
        seedDefaults();
        storage.saveAll(this.categories.values());
        setSettingsSlot(DEFAULT_SETTINGS_SLOT);
    }

    /**
     * Resets only the navigator layout — where categories and the settings button sit — without touching the categories
     * themselves. Built-in categories return to their default slots and become visible again; custom categories are kept
     * but removed from the navigator (so they can be re-added). The settings button returns to its default slot.
     */
    public void resetNavigatorLayout() {
        Map<String, NavigatorCategoryImpl> defaults = buildDefaults();
        for (NavigatorCategoryImpl category : this.categories.values()) {
            NavigatorCategoryImpl preset = defaults.get(category.getId());
            if (preset != null) {
                category.setNavigatorSlot(preset.getNavigatorSlot());
                category.setShownInNavigator(preset.isShownInNavigator());
            } else {
                category.setShownInNavigator(false);
            }
            storage.save(category);
        }
        setSettingsSlot(DEFAULT_SETTINGS_SLOT);
    }

    public NavigatorCategoryImpl createCategory(String displayName) {
        String id = StringUtils.uniqueId(displayName, "category", this.categories::containsKey);
        int slot = this.categories.values().stream()
                        .mapToInt(NavigatorCategory::getNavigatorSlot)
                        .max()
                        .orElse(10)
                + 1;
        NavigatorCategoryImpl category = NavigatorCategoryImpl.builder(id)
                .displayName(displayName)
                .navigatorSlot(slot)
                .build();
        put(category);
        storage.save(category);
        return category;
    }

    public void persist(NavigatorCategoryImpl category) {
        storage.save(category);
    }

    /**
     * Adds a status to the default category so a newly created status is reachable in the navigator out of the box.
     */
    public void addStatusToDefaultCategory(String statusId) {
        NavigatorCategoryImpl defaultSet = (NavigatorCategoryImpl) getDefaultCategory();
        defaultSet.addStatusId(statusId);
        storage.save(defaultSet);
    }

    /**
     * Removes a status id from every category that lists it, persisting each change. Called when a status is deleted, since a
     * shared status may be grouped by several categories.
     */
    public void removeStatusFromCategories(String statusId) {
        for (NavigatorCategoryImpl category : this.categories.values()) {
            if (category.getStatusIds().contains(statusId)) {
                category.removeStatusId(statusId);
                storage.save(category);
            }
        }
    }

    /**
     * Removes a category (built-in or custom). The last remaining category is never deleted, so a valid default always
     * exists; an admin can restore the built-ins with {@link #resetToDefaults()}. Worlds previously displayed in the
     * category simply resolve to another matching category (or the {@link #getDefaultCategory() default}) on the next
     * render; no status is orphaned because statuses are shared rather than owned by a category. Folders, which hold a
     * fixed category, are re-homed to the default so they (and the worlds they contain) stay reachable.
     *
     * @return {@code true} if the category was deleted, {@code false} if it was unknown or the last remaining category
     */
    public boolean deleteCategory(String id) {
        if (!this.categories.containsKey(id) || this.categories.size() <= 1) {
            return false;
        }
        this.categories.remove(id);
        storage.delete(id);
        rehomeFolders(id);
        return true;
    }

    /**
     * Re-homes every folder that belonged to the just-deleted category to the {@link #getDefaultCategory() default}, so
     * the folders (and the worlds they contain) stay reachable in the navigator instead of vanishing until the next
     * reload. Mirrors how deleting a status resets the worlds that used it. Whole subtrees move together because a
     * subfolder shares its parent's category.
     */
    private void rehomeFolders(String deletedId) {
        NavigatorCategory fallback = getDefaultCategory();
        var folderStorage = plugin.getWorldService().getFolderStorage();
        List<Folder> rehomed = new ArrayList<>();
        for (Folder folder : folderStorage.getFolders()) {
            if (folder.getCategory().getId().equals(deletedId)) {
                ((FolderImpl) folder).setCategory(fallback);
                rehomed.add(folder);
            }
        }
        if (!rehomed.isEmpty()) {
            folderStorage.save(rehomed);
        }
    }
}
