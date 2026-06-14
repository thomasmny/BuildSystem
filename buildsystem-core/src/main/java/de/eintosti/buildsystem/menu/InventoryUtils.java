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
package de.eintosti.buildsystem.menu;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import java.util.Arrays;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Stateless factory for the {@link ItemStack}s used by menus. Thin façade over {@link ItemBuilder} for the common
 * material/name/lore and skull cases; item builders that need plugin state (config, settings, scheduler) live on
 * {@link MenuItems}.
 */
@NullMarked
public final class InventoryUtils {

    private InventoryUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a player skull ItemStack with the given display name and lore.
     *
     * @param displayName The display name of the skull
     * @param profileable The profile to use for the skull
     * @param lore The lore of the skull
     * @return The created skull ItemStack
     */
    public static ItemStack createSkull(String displayName, Profileable profileable, List<String> lore) {
        return ItemBuilder.skull(profileable).name(displayName).lore(lore).build();
    }

    /**
     * Creates a player skull ItemStack with the given display name and lore.
     *
     * @param displayName The display name of the skull
     * @param profileable The profile to use for the skull
     * @param lore The lore of the skull as varargs
     * @return The created skull ItemStack
     */
    public static ItemStack createSkull(String displayName, Profileable profileable, String... lore) {
        return createSkull(displayName, profileable, Arrays.asList(lore));
    }
}
