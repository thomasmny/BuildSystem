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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Folder implements Displayable {

    private final String name;
    private final List<Displayable> contents;
    private final List<String> worlds;

    private XMaterial material;

    public Folder(String name) {
        this.name = name;
        this.contents = new ArrayList<>();
        this.worlds = new ArrayList<>();
        this.material = XMaterial.CHEST;
    }

    public Folder(String name, XMaterial material, List<String> worlds) {
        this.name = name;
        this.contents = new ArrayList<>(); //TODO
        this.worlds = worlds;
        this.material = material;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public XMaterial getMaterial() {
        return material;
    }

    public void setMaterial(XMaterial material) {
        this.material = material;
    }

    public List<String> getWorlds() {
        return worlds;
    }

    @Override
    public String getDisplayName(Player player) {
        return Messages.getString("folder_item_title", player,
                new AbstractMap.SimpleEntry<>("%folder%", name));
    }

    @Override
    public List<String> getLore(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.getString("folder_item_contents", player,
                new AbstractMap.SimpleEntry<>("%count%", String.valueOf(contents.size()))));
        return lore;
    }

    @Override
    public ItemStack asItemStack(Player player) {
        ItemStack item = new ItemStack(getMaterial().parseMaterial());
        item.setAmount(1);
        return item;
    }

    public void addContent(de.eintosti.buildsystem.navigator.model.Displayable content) {
        contents.add(content);
    }

    public void removeContent(de.eintosti.buildsystem.navigator.model.Displayable content) {
        contents.remove(content);
    }

    public List<Displayable> getContents() {
        return contents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Folder folder = (Folder) o;
        return name.equals(folder.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
} 