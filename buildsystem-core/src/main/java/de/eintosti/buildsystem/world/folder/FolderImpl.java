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
package de.eintosti.buildsystem.world.folder;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.world.WorldContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FolderImpl implements Folder {

    private final WorldContext context;
    private final UUID uuid;
    private String name;
    private final Builder creator;
    private final long creation;
    private NavigatorCategory category;
    private final Set<UUID> worlds;
    private final List<Folder> subfolders;

    private @Nullable Folder parent;

    private Material material;
    private @Nullable String iconSkullTexture;
    private String permission;
    private String project;

    /**
     * The stored category and icon as they were read, when they could not be resolved. They are saved in place of the
     * fallback in effect until the category or icon is set.
     */
    private @Nullable String unresolvedCategory;

    private @Nullable String unresolvedMaterial;

    public FolderImpl(
            WorldContext context, String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        this(builder(context, UUID.randomUUID())
                .name(name)
                .category(category)
                .parent(parent)
                .creator(creator));
    }

    private FolderImpl(FolderBuilder builder) {
        this.context = builder.context;
        this.uuid = builder.uuid;
        this.name = Objects.requireNonNull(builder.name, "name");
        this.creation = builder.creation;
        this.category = Objects.requireNonNull(builder.category, "category");
        this.parent = builder.parent;
        this.creator = Objects.requireNonNull(builder.creator, "creator");
        this.worlds = new LinkedHashSet<>(builder.worlds);
        this.material = builder.material;
        this.iconSkullTexture = builder.iconSkullTexture;
        this.permission = builder.permission;
        this.project = builder.project;
        this.subfolders = new ArrayList<>();
        if (parent != null) {
            ((FolderImpl) parent).addSubFolder(this);
        }
    }

    public static FolderBuilder builder(WorldContext context, UUID uuid) {
        return new FolderBuilder(context, uuid);
    }

    @Override
    public UUID getUniqueId() {
        return this.uuid;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Folder name must not be blank");
        }
        this.name = name;
    }

    @Override
    public String getDisplayName(Player player) {
        return context.messages().getString("folder_item_title", player, Placeholders.of("%folder%", name));
    }

    @Override
    public long getCreation() {
        return creation;
    }

    @Override
    public Builder getCreator() {
        return this.creator;
    }

    @Override
    public Material getIcon() {
        return this.material;
    }

    @Override
    public void setIcon(Material material) {
        this.material = material;
        this.unresolvedMaterial = null;
    }

    @Override
    public @Nullable String getIconSkullTexture() {
        return this.iconSkullTexture;
    }

    @Override
    public void setIconSkullTexture(@Nullable String skullTexture) {
        this.iconSkullTexture = skullTexture;
    }

    @Override
    public void addToInventory(Inventory inventory, int slot, Player player) {
        context.menuItems().renderDisplayable(inventory, slot, this, player);
    }

    @Override
    @Contract("_ -> new")
    public List<String> getLore(Player player) {
        return new ArrayList<>(context.messages()
                .getStringList(
                        "folder_item_lore",
                        player,
                        Placeholders.of()
                                .add("%permission%", this.permission)
                                .add("%project%", this.project)
                                .add("%worlds%", String.valueOf(getWorldCount()))
                                .build()));
    }

    @Override
    public NavigatorCategory getCategory() {
        return this.category;
    }

    /**
     * Reassigns this folder to another {@link NavigatorCategory}. Not part of the public {@link Folder} API; used by the
     * category registry to re-home folders when their category is deleted. Subfolders share their parent's category, so
     * the whole subtree must be moved together to keep the parent-category invariant enforced by {@link #setParent}.
     */
    public void setCategory(NavigatorCategory category) {
        this.category = category;
        this.unresolvedCategory = null;
    }

    /**
     * Moves the folder to {@code category} because its own was deleted. Unlike {@link #setCategory}, a stored category
     * that could not be resolved is kept, since the folder only showed the deleted one as a fallback.
     */
    public void rehome(NavigatorCategory category) {
        this.category = category;
    }

    /**
     * Keeps the stored category id and icon material that could not be resolved, to be saved instead of the fallbacks.
     */
    public void keepUnresolved(@Nullable String category, @Nullable String material) {
        this.unresolvedCategory = category;
        this.unresolvedMaterial = material;
    }

    public @Nullable String getUnresolvedCategory() {
        return unresolvedCategory;
    }

    public @Nullable String getUnresolvedMaterial() {
        return unresolvedMaterial;
    }

    @Override
    public @Nullable Folder getParent() {
        return this.parent;
    }

    @Override
    public void setParent(@Nullable Folder parent) {
        if (parent != null && !this.category.equals(parent.getCategory())) {
            throw new IllegalArgumentException("Cannot set parent folder: category mismatch (expected: %s, found: %s)"
                    .formatted(this.category.getId(), parent.getCategory().getId()));
        }

        if (parent != null) {
            ((FolderImpl) parent).addSubFolder(this);
        } else if (this.parent != null) {
            ((FolderImpl) this.parent).removeSubFolder(this);
        }

        this.parent = parent;
    }

    @Override
    public boolean hasParent() {
        return this.parent != null;
    }

    @Override
    @Unmodifiable
    public List<UUID> getWorldUUIDs() {
        return List.copyOf(this.worlds);
    }

    @Override
    public boolean containsWorld(BuildWorld buildWorld) {
        return containsWorld(buildWorld.getUniqueId());
    }

    @Override
    public boolean containsWorld(UUID uuid) {
        return this.worlds.contains(uuid);
    }

    @Override
    public void addWorld(BuildWorld buildWorld) {
        this.worlds.add(buildWorld.getUniqueId());
        // BuildWorld owns the back-reference; only update it when it is not already pointing here, which also
        // terminates the addWorld <-> setFolder handshake.
        if (buildWorld.getFolder() != this) {
            buildWorld.setFolder(this);
        }
    }

    @Override
    public void removeWorld(BuildWorld buildWorld) {
        this.worlds.remove(buildWorld.getUniqueId());
        if (buildWorld.getFolder() == this) {
            buildWorld.setFolder(null);
        }
    }

    @Override
    public void removeWorld(UUID uuid) {
        // Pure list cleanup for a world that no longer exists (no back-reference to clear).
        this.worlds.remove(uuid);
    }

    @Override
    @Unmodifiable
    public List<Folder> getSubFolders() {
        return Collections.unmodifiableList(this.subfolders);
    }

    /**
     * Adds {@code child} to this folder's sub-folder list, ignoring duplicates. Package-private: membership is driven by
     * {@link #setParent}, so a folder owns its own sub-folder list rather than having it mutated through a cast from
     * outside.
     */
    void addSubFolder(FolderImpl child) {
        if (!this.subfolders.contains(child)) {
            this.subfolders.add(child);
        }
    }

    /**
     * Removes {@code child} from this folder's sub-folder list. Counterpart to {@link #addSubFolder(FolderImpl)}.
     */
    void removeSubFolder(FolderImpl child) {
        this.subfolders.remove(child);
    }

    @Override
    public int getWorldCount() {
        int total = this.worlds.size();
        for (Folder subfolder : this.subfolders) {
            total += subfolder.getWorldCount();
        }
        return total;
    }

    @Override
    public String getPermission() {
        return this.permission;
    }

    @Override
    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public String getProject() {
        return project;
    }

    @Override
    public void setProject(String project) {
        this.project = project;
    }

    @Override
    public boolean canView(Player player) {
        return permission.equals("-")
                || player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)
                || player.hasPermission(permission);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        FolderImpl folder = (FolderImpl) other;
        return uuid.equals(folder.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * Builds a folder from its stored fields. Name, category and creator are required; the rest start at the values a
     * new folder gets.
     */
    public static final class FolderBuilder {

        private final WorldContext context;
        private final UUID uuid;
        private @Nullable String name;
        private long creation = System.currentTimeMillis();
        private @Nullable NavigatorCategory category;
        private @Nullable Folder parent;
        private @Nullable Builder creator;
        private Material material = Material.CHEST;
        private @Nullable String iconSkullTexture;
        private String permission = "-";
        private String project = "-";
        private List<UUID> worlds = List.of();

        private FolderBuilder(WorldContext context, UUID uuid) {
            this.context = context;
            this.uuid = uuid;
        }

        public FolderBuilder name(String name) {
            this.name = name;
            return this;
        }

        public FolderBuilder creation(long creation) {
            this.creation = creation;
            return this;
        }

        public FolderBuilder category(NavigatorCategory category) {
            this.category = category;
            return this;
        }

        public FolderBuilder parent(@Nullable Folder parent) {
            this.parent = parent;
            return this;
        }

        public FolderBuilder creator(Builder creator) {
            this.creator = creator;
            return this;
        }

        public FolderBuilder material(Material material) {
            this.material = material;
            return this;
        }

        public FolderBuilder iconSkullTexture(@Nullable String iconSkullTexture) {
            this.iconSkullTexture = iconSkullTexture;
            return this;
        }

        public FolderBuilder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public FolderBuilder project(String project) {
            this.project = project;
            return this;
        }

        public FolderBuilder worlds(List<UUID> worlds) {
            this.worlds = worlds;
            return this;
        }

        public FolderImpl build() {
            return new FolderImpl(this);
        }
    }
}
