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
package de.eintosti.buildsystem.api.world.display;

import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;

/**
 * A named, styled, admin-managed entry in a registry that is arranged on a menu grid — a world status or a navigator
 * category. Entries are identified by a stable {@link #getId() id}, occupy a {@link #getSlot() slot} in their editor's
 * layout, and can be hidden without being deleted.
 *
 * <p>This is deliberately not a {@link Displayable}: a displayable renders per viewer
 * ({@code getDisplayName(Player)}, {@code getLore(Player)}), whereas a registry entry is static configuration whose
 * name and colour do not depend on who is looking.
 *
 * @since 4.0.0
 */
@NullMarked
public interface RegistryEntry {

    /**
     * Gets the stable identifier for this entry, used in config, storage and permission nodes. Unlike the display
     * name, it does not change when the entry is restyled.
     *
     * @return The id
     */
    String getId();

    /**
     * Gets the name shown to players, without colour.
     *
     * @return The display name
     */
    String getDisplayName();

    /**
     * Gets the legacy colour code prefix applied to this entry's {@link #getStyledName() styled name}.
     *
     * @return The colour code
     */
    String getColor();

    /**
     * Gets the display name prefixed with this entry's {@link #getColor() colour}.
     *
     * @return The styled display name
     */
    default String getStyledName() {
        return getColor() + getDisplayName();
    }

    /**
     * Gets the material shown for this entry in menus.
     *
     * @return The icon material
     */
    Material getIcon();

    /**
     * Gets the slot this entry occupies in its editor's layout.
     *
     * @return The inventory slot
     */
    int getSlot();

    /**
     * Sets the slot this entry occupies in its editor's layout.
     *
     * @param slot The inventory slot
     */
    void setSlot(int slot);

    /**
     * Gets whether this entry is shown in its menu. A hidden entry still exists and can still be assigned; it is only
     * absent from the picker.
     *
     * @return {@code true} if shown
     */
    boolean isShown();

    /**
     * Sets whether this entry is shown in its menu.
     *
     * @param shown {@code true} to show it
     */
    void setShown(boolean shown);

    /**
     * Gets whether this entry ships with the plugin rather than having been created by an admin. Built-in entries can
     * be restyled and deleted like any other; the flag exists so "reset to defaults" knows what to restore.
     *
     * @return {@code true} if built in
     */
    boolean isBuiltIn();
}
