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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.codec.CategoryCodec;
import de.eintosti.buildsystem.storage.yaml.YamlRegistryStorage;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.WorldServiceImpl;
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
import java.util.function.Supplier;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns every {@link NavigatorCategory}, seeding the built-in defaults on first run and persisting all administrator
 * changes. The category a world is displayed in is resolved by matching both the category's
 * {@link NavigatorCategory#getVisibilities() visibilities} and its statuses against the world. Any category may be
 * deleted, including the last one — the navigator then simply shows no categories until {@link #resetToDefaults()}
 * restores the built-ins. As a safety net for folders (which always need a home category), {@link #getDefault()}
 * reseeds the built-ins on demand when the registry is empty. Deleting a category never orphans a status because statuses
 * are shared, not owned.
 */
@NullMarked
public class NavigatorCategoryRegistryImpl extends AbstractRegistry<NavigatorCategoryImpl>
        implements NavigatorCategoryRegistry {

    /** Slot the settings button occupies in a freshly seeded navigator (matches the historical fixed slot). */
    public static final int DEFAULT_SETTINGS_SLOT = 15;

    private static final String SETTINGS_SLOT_KEY = "settings-slot";

    private final BuildSystemPlugin plugin;
    private final Supplier<WorldServiceImpl> worldService;
    private int settingsSlot;

    public NavigatorCategoryRegistryImpl(BuildSystemPlugin plugin, Supplier<WorldServiceImpl> worldService) {
        super(
                new YamlRegistryStorage<>(
                        plugin, "categories.yml", "categories", "navigator category", new CategoryCodec()),
                Comparator.comparingInt(NavigatorCategoryImpl::getSlot));
        this.plugin = plugin;
        this.worldService = worldService;

        loadOrSeed();
        this.settingsSlot = storage.getInt(SETTINGS_SLOT_KEY, DEFAULT_SETTINGS_SLOT);
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
        storage.set(SETTINGS_SLOT_KEY, slot);
    }

    /**
     * {@return a fresh map of the built-in categories in their default state} Built on demand without touching the live
     * registry, so it can back both first-run seeding and the navigator-layout reset.
     */
    @Override
    protected Map<String, NavigatorCategoryImpl> buildDefaults() {
        Map<String, NavigatorCategoryImpl> defaults = new LinkedHashMap<>();
        List<String> activeStatuses = List.of("not_started", "in_progress", "almost_finished", "finished");
        // Built-in categories keep the pre-4.0 navigator icons: textured player-head skulls. The private category
        // defaults to the viewing player's own head ({@code %viewer%}).
        defaults.put(
                PUBLIC_ID,
                NavigatorCategoryImpl.builder(PUBLIC_ID)
                        .displayName("Worlds")
                        .color("&b")
                        .icon(Material.PLAYER_HEAD)
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
                        .icon(Material.PLAYER_HEAD)
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
                        .icon(Material.PLAYER_HEAD)
                        .iconSkullTexture(ItemBuilder.VIEWER_HEAD)
                        .visibilities(EnumSet.of(Visibility.ADDED_PLAYERS))
                        .navigatorSlot(13)
                        .builtIn(true)
                        .statusIds(activeStatuses)
                        .build());
        return defaults;
    }

    @Override
    public Collection<NavigatorCategory> getAll() {
        return Collections.unmodifiableList(sorted());
    }

    @Override
    public Optional<NavigatorCategory> get(@Nullable String id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public NavigatorCategory getCategoryForWorld(BuildWorld world) {
        WorldData data = world.getData();
        String statusId = data.get(WorldDataKey.STATUS).getId();
        for (NavigatorCategory category : getAll()) {
            if (category.getVisibilities().contains(data.get(WorldDataKey.VISIBILITY))
                    && category.getStatusIds().contains(statusId)) {
                return category;
            }
        }
        return getDefault();
    }

    @Override
    protected String preferredDefaultId() {
        // Folders always need a home category, so getDefault() reseeds an empty registry. The navigator's own browse
        // paths never call it, so deleting every category still leaves the navigator empty until an admin resets.
        return PUBLIC_ID;
    }

    @Override
    protected boolean mayBeEmpty() {
        return true;
    }

    @Override
    protected void onDiscarded(String id) {
        rehomeFolders(id);
    }

    /** Also puts the settings button back in its default slot. */
    @Override
    public void resetToDefaults() {
        super.resetToDefaults();
        setSettingsSlot(DEFAULT_SETTINGS_SLOT);
    }

    /** Also puts the settings button back in its default slot. */
    @Override
    public void resetLayout() {
        super.resetLayout();
        setSettingsSlot(DEFAULT_SETTINGS_SLOT);
    }

    public NavigatorCategoryImpl create(String displayName) {
        String id = StringUtils.uniqueId(displayName, "category", entries::containsKey);
        int slot = entries.values().stream()
                        .mapToInt(NavigatorCategory::getSlot)
                        .max()
                        .orElse(10)
                + 1;
        NavigatorCategoryImpl category = NavigatorCategoryImpl.builder(id)
                .displayName(displayName)
                .navigatorSlot(slot)
                .build();
        entries.put(id, category);
        storage.save(category);
        return category;
    }

    @Override
    public void persist(NavigatorCategory category) {
        save(category);
    }

    /**
     * Adds a status to the default category so a newly created status is reachable in the navigator out of the box.
     */
    public void addStatusToDefaultCategory(String statusId) {
        // Don't resurrect the built-ins just because a status was created: if an admin has deleted every category, a
        // new
        // status simply stays ungrouped until a category is created (or the built-ins are reset) to list it.
        if (entries.isEmpty()) {
            return;
        }
        NavigatorCategoryImpl defaultSet = (NavigatorCategoryImpl) getDefault();
        defaultSet.addStatusId(statusId);
        storage.save(defaultSet);
    }

    /**
     * Removes a status id from every category that lists it, persisting each change. Called when a status is deleted, since a
     * shared status may be grouped by several categories.
     */
    public void removeStatusFromCategories(String statusId) {
        for (NavigatorCategoryImpl category : entries.values()) {
            if (category.getStatusIds().contains(statusId)) {
                category.removeStatusId(statusId);
                storage.save(category);
            }
        }
    }

    /**
     * Re-homes every folder that belonged to the just-deleted category to the {@link #getDefault() default}, so
     * the folders (and the worlds they contain) stay reachable in the navigator instead of vanishing until the next
     * reload. Mirrors how deleting a status resets the worlds that used it. Whole subtrees move together because a
     * subfolder shares its parent's category.
     */
    private void rehomeFolders(String deletedId) {
        NavigatorCategory fallback = getDefault();
        FolderStorageImpl folderStorage = worldService.get().getFolderStorage();
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
