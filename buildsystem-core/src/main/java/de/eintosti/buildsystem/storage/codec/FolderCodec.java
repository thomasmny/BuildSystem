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

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.util.MaterialUtils;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Codec} for {@link Folder}s. Since v4 the section is keyed by the folder's UUID, the name is carried as a
 * {@code name} field, and the parent is referenced by UUID.
 *
 * <p>A folder's parent cannot be resolved from a single section, so {@link #deserialize(String, ConfigurationSection)}
 * leaves it unset and {@link #parentReference(ConfigurationSection)} hands the stored reference to the storage, which
 * links it once every folder is loaded.
 */
@NullMarked
public final class FolderCodec implements Codec<Folder> {

    private static final String PARENT = "parent";

    private final WorldContext context;
    private final NavigatorCategoryRegistry categoryRegistry;

    public FolderCodec(WorldContext context, NavigatorCategoryRegistry categoryRegistry) {
        this.context = context;
        this.categoryRegistry = categoryRegistry;
    }

    @Override
    public String key(Folder value) {
        return value.getUniqueId().toString();
    }

    @Override
    public Map<String, Object> serialize(Folder folder) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("name", folder.getName());
        serialized.put("uuid", folder.getUniqueId().toString());
        serialized.put("creator", folder.getCreator().toString());
        serialized.put("creation", folder.getCreation());
        // A subfolder always has its parent's category, so a whole tree saves its top folder's, which keeps an
        // unresolved category the same across the tree.
        FolderImpl top = (FolderImpl) folder;
        while (top.getParent() != null) {
            top = (FolderImpl) top.getParent();
        }
        serialized.put(
                "category",
                Objects.requireNonNullElse(
                        top.getUnresolvedCategory(), folder.getCategory().getId()));
        if (folder.hasParent()) {
            serialized.put(PARENT, folder.getParent().getUniqueId().toString());
        }
        serialized.put(
                "material",
                Objects.requireNonNullElse(
                        ((FolderImpl) folder).getUnresolvedMaterial(),
                        folder.getIcon().name()));
        Codec.putIfPresent(serialized, "icon-skull-texture", folder.getIconSkullTexture());
        serialized.put("permission", folder.getPermission());
        serialized.put("project", folder.getProject());
        serialized.put(
                "worlds", folder.getWorldUUIDs().stream().map(UUID::toString).toList());
        return serialized;
    }

    @Override
    public FolderImpl deserialize(String key, ConfigurationSection section) {
        // The name falls back to the key, which held it before v4 keyed folders by UUID.
        String name = section.getString("name", key);
        String categoryId = section.getString("category");
        NavigatorCategory category = parseCategory(categoryId);
        String materialName = section.getString("material");
        Material material = MaterialUtils.match(materialName);
        FolderImpl folder = FolderImpl.builder(context, UUID.fromString(key))
                .name(name)
                .creator(Objects.requireNonNull(
                        Builder.deserialize(section.getString("creator")),
                        "Creator cannot be null for folder: " + name))
                .creation(section.getLong("creation", System.currentTimeMillis()))
                .category(category != null ? category : categoryRegistry.getDefault())
                .material(material != null ? material : Material.CHEST)
                .iconSkullTexture(section.getString("icon-skull-texture"))
                .permission(section.getString("permission", "-"))
                .project(section.getString("project", "-"))
                .worlds(section.getStringList("worlds").stream()
                        .map(UUID::fromString)
                        .toList())
                .build();
        // Kept as read so that saving does not replace them with the fallbacks.
        folder.keepUnresolved(category == null ? categoryId : null, material == null ? materialName : null);
        return folder;
    }

    /**
     * Returns the stored parent-folder reference (the parent's UUID) for the storage to link in its second load pass.
     *
     * @param section The folder's configuration section
     * @return The parent folder's UUID, or {@code null} when the folder has no parent
     */
    public static @Nullable String parentReference(ConfigurationSection section) {
        return section.getString(PARENT);
    }

    /**
     * Resolves the stored category id. Pre-4.0 stored this as an upper-case enum name ({@code PUBLIC}, {@code ARCHIVE}
     * or {@code PRIVATE}), which lower-casing turns into the built-in category id.
     *
     * @return The category, or {@code null} when the key is missing or unknown
     */
    private @Nullable NavigatorCategory parseCategory(@Nullable String categoryId) {
        return categoryId == null
                ? null
                : categoryRegistry.get(categoryId.toLowerCase(Locale.ROOT)).orElse(null);
    }
}
