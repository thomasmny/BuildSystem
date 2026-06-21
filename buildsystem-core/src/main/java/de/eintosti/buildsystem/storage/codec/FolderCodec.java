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
package de.eintosti.buildsystem.storage.codec;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Codec} for {@link Folder}s, mapping a folder to and from its section. Since v4 the section is keyed by the
 * folder's UUID, the name is carried as a {@code name} field, and the parent is referenced by UUID.
 *
 * <p>A folder's parent cannot be resolved from a single section in isolation; {@link #deserialize(String,
 * ConfigurationSection)} therefore leaves the parent unset and exposes the raw reference through
 * {@link #parentReference(ConfigurationSection)} for the storage's second load pass to link.
 */
@NullMarked
public final class FolderCodec implements Codec<Folder> {

    private static final String NAME = "name";
    private static final String UUID_KEY = "uuid";
    private static final String CREATOR = "creator";
    private static final String CREATION = "creation";
    private static final String CATEGORY = "category";
    private static final String PARENT = "parent";
    private static final String MATERIAL = "material";
    private static final String ICON_SKULL_TEXTURE = "icon-skull-texture";
    private static final String PERMISSION = "permission";
    private static final String PROJECT = "project";
    private static final String WORLDS = "worlds";

    private final BuildSystemPlugin plugin;

    public FolderCodec(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String key(Folder value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, @Nullable Object> serialize(Folder folder) {
        Map<String, @Nullable Object> serialized = new HashMap<>();
        serialized.put(NAME, folder.getName());
        serialized.put(UUID_KEY, folder.getUniqueId().toString());
        serialized.put(CREATOR, folder.getCreator().toString());
        serialized.put(CREATION, folder.getCreation());
        serialized.put(CATEGORY, folder.getCategory().getId());
        serialized.put(
                PARENT, folder.hasParent() ? folder.getParent().getUniqueId().toString() : null);
        serialized.put(MATERIAL, folder.getIcon().name());
        serialized.put(ICON_SKULL_TEXTURE, folder.getIconSkullTexture());
        serialized.put(PERMISSION, folder.getPermission());
        serialized.put(PROJECT, folder.getProject());
        serialized.put(
                WORLDS, folder.getWorldUUIDs().stream().map(UUID::toString).toList());
        return serialized;
    }

    @Override
    public FolderImpl deserialize(String key, ConfigurationSection section) {
        // v4 keys sections by UUID and carries the name as a field; fall back to the key for pre-migration safety.
        UUID uuid = UUID.fromString(key);
        String name = section.getString(NAME, key);
        Builder creator = Objects.requireNonNull(
                Builder.deserialize(section.getString(CREATOR)), "Creator cannot be null for folder: " + name);
        long creation = section.getLong(CREATION, System.currentTimeMillis());
        NavigatorCategory category = resolveCategory(section);
        XMaterial defaultMaterial = XMaterial.CHEST;
        XMaterial material = XMaterial.matchXMaterial(section.getString(MATERIAL, defaultMaterial.name()))
                .orElse(defaultMaterial);
        String permission = section.getString(PERMISSION, "-");
        String project = section.getString(PROJECT, "-");
        List<UUID> worlds =
                section.getStringList(WORLDS).stream().map(UUID::fromString).toList();

        FolderImpl folder = new FolderImpl(
                WorldContext.fromPlugin(plugin),
                uuid,
                name,
                creation,
                category,
                null, // Parent is linked by the storage's second load pass.
                creator,
                material,
                permission,
                project,
                worlds,
                new ArrayList<>());
        folder.setIconSkullTexture(section.getString(ICON_SKULL_TEXTURE));
        return folder;
    }

    /**
     * Returns the stored parent-folder reference (the parent's name) for the storage to link in its second load pass.
     *
     * @param section The folder's configuration section
     * @return The parent folder's name, or {@code null} when the folder has no parent
     */
    public @Nullable String parentReference(ConfigurationSection section) {
        return section.getString(PARENT);
    }

    /**
     * Resolves a folder's {@link NavigatorCategory} from its stored {@code category} id. Pre-4.0 stored this as an
     * upper-case enum name ({@code PUBLIC}/{@code ARCHIVE}/{@code PRIVATE}), which lower-casing normalises to the
     * built-in category id. Falls back to the default category when the key is missing or unknown.
     */
    private NavigatorCategory resolveCategory(ConfigurationSection section) {
        NavigatorCategoryRegistry registry = plugin.getNavigatorCategoryRegistry();
        String categoryId = section.getString(CATEGORY);
        categoryId = categoryId != null ? categoryId.toLowerCase(Locale.ROOT) : null;
        return categoryId != null
                ? registry.getCategory(categoryId).orElseGet(registry::getDefaultCategory)
                : registry.getDefaultCategory();
    }
}
