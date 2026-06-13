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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;

/**
 * Stateless factory for the {@link ItemStack}s used by menus. Item builders that need plugin state (config, settings,
 * scheduler) live on {@link MenuItems}.
 */
@NullMarked
public final class InventoryUtils {

    private static final Logger LOGGER = Logger.getLogger(InventoryUtils.class.getName());

    private InventoryUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates an ItemStack with the given material, display name and lore.
     *
     * @param material The material of the item
     * @param displayName The display name of the item
     * @param lore The lore of the item
     * @return The created ItemStack
     */
    public static ItemStack createItem(XMaterial material, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        if (itemStack == null) {
            itemStack = XMaterial.BEDROCK.parseItem();
            LOGGER.warning("Unknown material found (" + material + "). Defaulting to " + itemStack.getType() + ".");
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Creates an ItemStack with the given material, display name and lore.
     *
     * @param material The material of the item
     * @param displayName The display name of the item
     * @param lore The lore of the item as varargs
     * @return The created ItemStack
     */
    public static ItemStack createItem(XMaterial material, String displayName, String... lore) {
        return createItem(material, displayName, Arrays.asList(lore));
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
        ItemStack skull = XSkull.createItem().profile(profileable).lenient().apply();

        ItemMeta itemMeta = skull.getItemMeta();
        if (itemMeta == null) {
            return skull;
        }

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        skull.setItemMeta(itemMeta);

        return skull;
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
