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
import de.eintosti.buildsystem.storage.codec.FieldCodec.Field;
import de.eintosti.buildsystem.util.MaterialUtils;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import de.eintosti.buildsystem.world.folder.FolderImpl.FolderBuilder;
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

    private static final String NAME = "name";
    private static final String PARENT = "parent";

    private final WorldContext context;
    private final NavigatorCategoryRegistry categoryRegistry;
    private final FieldCodec<Folder, FolderBuilder> fields = new FieldCodec<>(
            new Field<>(NAME, Folder::getName, FolderCodec::readName, FolderBuilder::name),
            FieldCodec.writeOnly("uuid", folder -> folder.getUniqueId().toString()),
            new Field<>(
                    "creator",
                    folder -> folder.getCreator().toString(),
                    FolderCodec::readCreator,
                    FolderBuilder::creator),
            new Field<>(
                    "creation",
                    Folder::getCreation,
                    (section, key) -> section.getLong(key, System.currentTimeMillis()),
                    FolderBuilder::creation),
            new Field<>(
                    "category", folder -> folder.getCategory().getId(), this::readCategory, FolderBuilder::category),
            FieldCodec.writeOnly(
                    PARENT,
                    folder -> folder.hasParent()
                            ? folder.getParent().getUniqueId().toString()
                            : null),
            new Field<>(
                    "material", folder -> folder.getIcon().name(), FolderCodec::readMaterial, FolderBuilder::material),
            FieldCodec.optionalString(
                    "icon-skull-texture", Folder::getIconSkullTexture, FolderBuilder::iconSkullTexture),
            FieldCodec.string("permission", "-", Folder::getPermission, FolderBuilder::permission),
            FieldCodec.string("project", "-", Folder::getProject, FolderBuilder::project),
            new Field<>(
                    "worlds",
                    folder ->
                            folder.getWorldUUIDs().stream().map(UUID::toString).toList(),
                    (section, key) -> section.getStringList(key).stream()
                            .map(UUID::fromString)
                            .toList(),
                    FolderBuilder::worlds));

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
        return fields.serialize(folder);
    }

    @Override
    public FolderImpl deserialize(String key, ConfigurationSection section) {
        FolderBuilder builder = FolderImpl.builder(context, UUID.fromString(key));
        fields.read(section, builder);
        return builder.build();
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
     * Falls back to the section key, which held the name before v4 keyed folders by UUID.
     */
    private static String readName(ConfigurationSection section, String key) {
        return section.getString(key, section.getName());
    }

    private static Builder readCreator(ConfigurationSection section, String key) {
        return Objects.requireNonNull(
                Builder.deserialize(section.getString(key)),
                "Creator cannot be null for folder: " + readName(section, NAME));
    }

    private static Material readMaterial(ConfigurationSection section, String key) {
        return Objects.requireNonNullElse(MaterialUtils.match(section.getString(key)), Material.CHEST);
    }

    /**
     * Resolves the stored category id. Pre-4.0 stored this as an upper-case enum name ({@code PUBLIC}, {@code ARCHIVE}
     * or {@code PRIVATE}), which lower-casing turns into the built-in category id. Falls back to the default category
     * when the key is missing or unknown.
     */
    private NavigatorCategory readCategory(ConfigurationSection section, String key) {
        String categoryId = section.getString(key);
        return categoryId == null
                ? categoryRegistry.getDefault()
                : categoryRegistry.get(categoryId.toLowerCase(Locale.ROOT)).orElseGet(categoryRegistry::getDefault);
    }
}
