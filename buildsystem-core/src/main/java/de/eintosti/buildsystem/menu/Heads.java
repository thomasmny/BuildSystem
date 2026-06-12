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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Displayable.DisplayableType;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Heads {

    private Heads() {}

    /**
     * Sets a world-icon item at the given slot, loading the skull texture asynchronously.
     * If the player has closed the inventory before the async load completes, the update is skipped.
     */
    public static void setWorldItem(JavaPlugin plugin, Player player, Inventory inventory, int slot,
                                    BuildWorld buildWorld, String displayName, List<String> lore) {
        XMaterial material = buildWorld.getData().material().get();
        if (material != XMaterial.PLAYER_HEAD) {
            inventory.setItem(slot, ItemBuilder.of(material).name(displayName).lore(lore).build());
            return;
        }

        ItemStack defaultHead = ItemBuilder.of(XMaterial.PLAYER_HEAD)
                .name(displayName)
                .lore(lore)
                .build();
        storeWorldInfo(defaultHead, buildWorld);
        inventory.setItem(slot, defaultHead);

        XSkull.createItem()
                .profile(buildWorld.getData().privateWorld().get()
                        ? buildWorld.asProfilable()
                        : Profileable.username(buildWorld.getName()))
                .fallback(buildWorld.asProfilable())
                .lenient()
                .applyAsync()
                .thenAcceptAsync(itemStack -> {
                    ItemMeta meta = itemStack.getItemMeta();
                    meta.setDisplayName(displayName);
                    meta.setLore(lore);
                    itemStack.setItemMeta(meta);
                    storeWorldInfo(itemStack, buildWorld);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Skip if the player has already closed this inventory
                        if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                            inventory.setItem(slot, itemStack);
                        }
                    });
                });
    }

    private static void storeWorldInfo(ItemStack itemStack, BuildWorld buildWorld) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(
                InventoryUtils.DISPLAYABLE_TYPE_KEY, PersistentDataType.STRING, DisplayableType.BUILD_WORLD.name());
        meta.getPersistentDataContainer().set(
                InventoryUtils.DISPLAYABLE_NAME_KEY, PersistentDataType.STRING, buildWorld.getName());
        itemStack.setItemMeta(meta);
    }
}
