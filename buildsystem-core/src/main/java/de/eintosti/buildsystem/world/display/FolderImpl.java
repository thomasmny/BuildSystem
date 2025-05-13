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
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

public class FolderImpl implements Folder {

    private final String name;
    private final List<String> worldNames;

    private XMaterial material;

    public FolderImpl(String name) {
        this.name = name;
        this.worldNames = new ArrayList<>();
        this.material = XMaterial.CHEST;
    }

    public FolderImpl(String name, XMaterial material, List<String> worldNames) {
        this.name = name;
        this.worldNames = worldNames;
        this.material = material;
    }

    @Override
    public String getName() {
        return name;
    }

    @Unmodifiable
    public List<String> getWorlds() {
        return Collections.unmodifiableList(worldNames);
    }

    public boolean containsWorld(String worldName) {
        return worldNames.contains(worldName);
    }

    public void addWorld(String worldName) {
        worldNames.add(worldName);
    }

    public void removeWorld(String worldName) {
        worldNames.remove(worldName);
    }

    public int getWorldCount() {
        return worldNames.size();
    }

    @Override
    public XMaterial getMaterial() {
        return material;
    }

    public void setMaterial(XMaterial material) {
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
        lore.add(Messages.getString("folder_item_contents", player,
                new AbstractMap.SimpleEntry<>("%count%", String.valueOf(getWorldCount())))
        );
        return lore;
    }

    @Override
    public ItemStack asItemStack(Player player) {
        return getMaterial().parseItem();
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