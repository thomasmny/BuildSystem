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

import de.eintosti.buildsystem.api.world.display.RegistryEntry;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;

/**
 * What statuses and navigator categories share: an id, whether it is built in, how it is shown, and where it sits in
 * its menu.
 */
@NullMarked
public abstract class RegistryEntryImpl implements RegistryEntry {

    private final String id;
    private final boolean builtIn;

    private String displayName;
    private String color;
    private Material icon;
    private int slot;
    private boolean shown;

    protected RegistryEntryImpl(
            String id, boolean builtIn, String displayName, String color, Material icon, int slot, boolean shown) {
        this.id = id;
        this.builtIn = builtIn;
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.slot = slot;
        this.shown = shown;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    /** {@return the slot in its menu, or {@code -1} for none} */
    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    /** {@return whether it is placed in its menu} */
    public boolean isShown() {
        return shown;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
