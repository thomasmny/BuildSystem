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
import de.eintosti.buildsystem.api.navigator.settings.NavigatorCategory;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public class FolderImpl implements Folder {

    private final FolderStorageImpl folderStorage;

    private final String name;
    private final NavigatorCategory category;
    private final List<UUID> worlds;

    private Folder parent;
    private XMaterial material;

    public FolderImpl(FolderStorageImpl folderStorage, String name, NavigatorCategory category, @Nullable Folder parent) {
        this(folderStorage, name, category, parent, XMaterial.CHEST, new ArrayList<>());
    }

    public FolderImpl(FolderStorageImpl folderStorage, String name, NavigatorCategory category, Folder parent, XMaterial material, List<UUID> worlds) {
        this.folderStorage = folderStorage;
        this.name = name;
        this.category = category;
        this.parent = parent;
        this.worlds = worlds;
        this.material = material;
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
        this.worlds.add(buildWorld.getUniqueId());
        this.folderStorage.assignWorldToFolder(buildWorld, this.name);
    }

    @Override
    public void removeWorld(BuildWorld buildWorld) {
        this.worlds.remove(buildWorld.getUniqueId());
        this.folderStorage.unassignWorldToFolder(buildWorld);
    }

    @Override
    public int getWorldCount() {
        return this.worlds.size();
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
    public String getDisplayName(Player player) {
        return Messages.getString("folder_item_title", player,
                new AbstractMap.SimpleEntry<>("%folder%", name)
        );
    }

    @Override
    public List<String> getLore(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.getString("folder_item_lore", player,
                new AbstractMap.SimpleEntry<>("%worlds%", String.valueOf(getWorldCount())))
        );
        return lore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FolderImpl folder = (FolderImpl) o;
        return name.equals(folder.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
} 