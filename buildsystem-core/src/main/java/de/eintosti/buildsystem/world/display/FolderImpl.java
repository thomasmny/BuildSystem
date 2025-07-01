/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FolderImpl implements Folder {

    private final String name;
    private final Builder creator;
    private final long creation;
    private final NavigatorCategory category;
    private final List<UUID> worlds;

    @Nullable
    private Folder parent;
    private XMaterial material;
    private String permission;
    private String project;

    public FolderImpl(String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        this(name, System.currentTimeMillis(), category, parent, creator, XMaterial.CHEST, "-", "-", new ArrayList<>());
    }

    public FolderImpl(
            String name,
            long creation,
            NavigatorCategory category,
            @Nullable Folder parent,
            Builder creator,
            XMaterial material,
            String permission,
            String project,
            List<UUID> worlds
    ) {
        this.name = name;
        this.creation = creation;
        this.category = category;
        this.parent = parent;
        this.creator = creator;
        this.worlds = worlds;
        this.material = material;
        this.permission = permission;
        this.project = project;
    }

    @Override
    public Builder getCreator() {
        return this.creator;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public NavigatorCategory getCategory() {
        return this.category;
    }

    @Override
    @Nullable
    public Folder getParent() {
        return this.parent;
    }

    @Override
    public boolean hasParent() {
        return this.parent != null;
    }

    @Override
    public void setParent(@Nullable Folder parent) {
        this.parent = parent;
    }

    @Override
    @Unmodifiable
    public List<UUID> getWorldUUIDs() {
        return Collections.unmodifiableList(this.worlds);
    }

    @Override
    public boolean containsWorld(BuildWorld buildWorld) {
        return this.worlds.contains(buildWorld.getUniqueId());
    }

    @Override
    public void addWorld(BuildWorld buildWorld) {
        if (containsWorld(buildWorld)) {
            return;
        }
        this.worlds.add(buildWorld.getUniqueId());
        buildWorld.setFolder(this);
    }

    @Override
    public void removeWorld(BuildWorld buildWorld) {
        if (!containsWorld(buildWorld)) {
            return;
        }
        this.worlds.remove(buildWorld.getUniqueId());
        buildWorld.setFolder(null);
    }

    @Override
    public int getWorldCount() {
        return this.worlds.size();
    }

    @Override
    public String getDisplayName(Player player) {
        return Messages.getString("folder_item_title", player,
                Map.entry("%folder%", name)
        );
    }

    @Override
    public long getCreation() {
        return creation;
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
        WorldPermissions permissions = WorldPermissionsImpl.of(null);
        if (permissions.hasAdminPermission(player) || permissions.canBypassViewPermission(player)) {
            return true;
        }

        if (this.permission.equals("-")) {
            return true;
        }

        return player.hasPermission(this.permission);
    }

    @Override
    public List<String> getLore(Player player) {
        return new ArrayList<>(Messages.getStringList("folder_item_lore", player,
                Map.entry("%permission%", this.permission),
                Map.entry("%project%", this.project),
                Map.entry("%worlds%", String.valueOf(getWorldCount())))
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        FolderImpl folder = (FolderImpl) other;
        return name.equals(folder.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}