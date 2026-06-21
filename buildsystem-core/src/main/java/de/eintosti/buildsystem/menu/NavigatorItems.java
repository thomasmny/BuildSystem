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
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns the navigator hotbar item — the item players use to open the navigator. Tags it with a persistent key so it can
 * be recognised again, and finds, creates and replaces it in a player's inventory.
 */
@NullMarked
public final class NavigatorItems {

    private final ConfigService configService;
    private final Messages messages;
    private final NamespacedKey navigatorKey;

    public NavigatorItems(JavaPlugin plugin, ConfigService configService, Messages messages) {
        this.configService = configService;
        this.messages = messages;
        this.navigatorKey = new NamespacedKey(plugin, "navigator");
    }

    /**
     * {@return a tagged navigator item for the player}
     *
     * @param player The player the item is created for
     */
    @Contract("_ -> new")
    public ItemStack create(Player player) {
        ItemStack itemStack = ItemBuilder.of(
                        configService.current().settings().navigator().item())
                .name(messages.getString("navigator_item", player))
                .build();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        itemMeta.getPersistentDataContainer().set(navigatorKey, PersistentDataType.BOOLEAN, true);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * {@return whether the item is a navigator} Matches both the configured material and the persistent tag, so a plain
     * copy of the material is not mistaken for one.
     *
     * @param itemStack The item to test
     */
    public boolean is(@Nullable ItemStack itemStack) {
        if (itemStack == null
                || itemStack.getType()
                        != configService.current().settings().navigator().item().get()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }

        return Boolean.TRUE.equals(itemMeta.getPersistentDataContainer().get(navigatorKey, PersistentDataType.BOOLEAN));
    }

    /**
     * {@return the inventory slots holding a navigator item}
     *
     * @param player The player whose inventory is scanned
     */
    @Unmodifiable
    public List<Integer> slots(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        return IntStream.range(0, playerInventory.getSize())
                .filter(i -> is(playerInventory.getItem(i)))
                .boxed()
                .toList();
    }

    /**
     * {@return whether the player is carrying a navigator}
     *
     * @param player The player whose inventory is scanned
     */
    public boolean has(Player player) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (is(itemStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces the first matching item in the player's inventory, falling back to slot 8 (or any free slot) when none
     * matches, so toggling the navigator item never drops it on the floor.
     *
     * @param player The player whose inventory is modified
     * @param findItemName The display name of the item to replace
     * @param findItemType The material of the item to replace
     * @param replaceItem The replacement item
     */
    public void replace(Player player, String findItemName, XMaterial findItemType, ItemStack replaceItem) {
        PlayerInventory inventory = player.getInventory();

        OptionalInt slot = IntStream.range(0, inventory.getSize())
                .filter(i -> {
                    ItemStack currentItem = inventory.getItem(i);
                    if (currentItem == null || currentItem.getType() != findItemType.get()) {
                        return false;
                    }

                    ItemMeta itemMeta = currentItem.getItemMeta();
                    return itemMeta != null && itemMeta.getDisplayName().equals(findItemName);
                })
                .findFirst();

        if (slot.isPresent()) {
            inventory.setItem(slot.getAsInt(), replaceItem);
            return;
        }

        ItemStack slot8 = inventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            inventory.setItem(8, replaceItem);
        } else {
            inventory.addItem(replaceItem);
        }
    }
}
