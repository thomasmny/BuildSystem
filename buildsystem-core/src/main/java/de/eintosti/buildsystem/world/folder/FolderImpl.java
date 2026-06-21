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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import java.util.*;
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
    private final List<UUID> worlds;
    private final List<Folder> subfolders;

    private @Nullable Folder parent;

    private XMaterial material;
    private @Nullable String iconSkullTexture;
    private String permission;
    private String project;

    public FolderImpl(
            WorldContext context, String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        this(
                context,
                UUID.randomUUID(),
                name,
                System.currentTimeMillis(),
                category,
                parent,
                creator,
                XMaterial.CHEST,
                "-",
                "-",
                new ArrayList<>(),
                new ArrayList<>());
    }

    public FolderImpl(
            WorldContext context,
            UUID uuid,
            String name,
            long creation,
            NavigatorCategory category,
            @Nullable Folder parent,
            Builder creator,
            XMaterial material,
            String permission,
            String project,
            List<UUID> worlds,
            List<Folder> subfolders) {
        this.context = context;
        this.uuid = uuid;
        this.name = name;
        this.creation = creation;
        this.category = category;
        this.parent = parent;
        if (parent != null && !parent.getSubFolders().contains(this)) {
            ((FolderImpl) parent).subfolders.add(this);
        }
        this.creator = creator;
        this.worlds = new ArrayList<>(worlds);
        this.material = material;
        this.permission = permission;
        this.project = project;
        this.subfolders = subfolders;
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
        return context.messages().getString("folder_item_title", player, Map.entry("%folder%", name));
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
    public XMaterial getIcon() {
        return this.material;
    }

    @Override
    public void setIcon(XMaterial material) {
        this.material = material;
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
                        Map.entry("%permission%", this.permission),
                        Map.entry("%project%", this.project),
                        Map.entry("%worlds%", String.valueOf(getWorldCount()))));
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

        if (parent != null && !parent.getSubFolders().contains(this)) {
            ((FolderImpl) parent).subfolders.add(this);
        } else if (parent == null && this.parent != null) {
            ((FolderImpl) this.parent).subfolders.remove(this);
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
        return Collections.unmodifiableList(this.worlds);
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
        if (!this.worlds.contains(buildWorld.getUniqueId())) {
            this.worlds.add(buildWorld.getUniqueId());
        }
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
        // We can pass null as a world since we are only checking for bypass permissions
        WorldPermissions permissions = WorldPermissionsImpl.of(context, null);
        if (permissions.hasAdminPermission(player) || permissions.canBypassViewPermission(player)) {
            return true;
        }

        if (this.permission.equals("-")) {
            return true;
        }

        return player.hasPermission(this.permission);
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
}
