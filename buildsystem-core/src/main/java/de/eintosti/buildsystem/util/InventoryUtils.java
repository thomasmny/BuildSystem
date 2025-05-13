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
package de.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.inventory.XInventoryView;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.player.settings.DesignColor;
import de.eintosti.buildsystem.player.settings.Settings;
import de.eintosti.buildsystem.world.BuildWorld;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for inventory-related operations. All methods are static and the class cannot be instantiated.
 */
public final class InventoryUtils {

    private static final BuildSystem plugin = BuildSystem.getPlugin(BuildSystem.class);
    private static final Logger LOGGER = plugin.getLogger();

    private InventoryUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates an ItemStack with the given material, display name and lore.
     *
     * @param material    The material of the item
     * @param displayName The display name of the item
     * @param lore        The lore of the item
     * @return The created ItemStack
     */
    public static ItemStack createItem(XMaterial material, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        if (itemStack == null) {
            LOGGER.warning("Unknown material found (" + material + "). Defaulting to BEDROCK.");
            itemStack = XMaterial.BEDROCK.parseItem();
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
     * @param material    The material of the item
     * @param displayName The display name of the item
     * @param lore        The lore of the item as varargs
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
     * @param lore        The lore of the skull
     * @return The created skull ItemStack
     */
    public static ItemStack createSkull(String displayName, Profileable profileable, List<String> lore) {
        ItemStack skull = XSkull.createItem()
                .profile(profileable)
                .lenient()
                .apply();

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
     * @param lore        The lore of the skull as varargs
     * @return The created skull ItemStack
     */
    public static ItemStack createSkull(String displayName, Profileable profileable, String... lore) {
        return createSkull(displayName, profileable, Arrays.asList(lore));
    }

    /**
     * Adds a glass pane to the given inventory at the specified position.
     *
     * @param player    The player viewing the inventory
     * @param inventory The inventory to add the glass pane to
     * @param position  The position to add the glass pane at
     */
    public static void addGlassPane(Player player, Inventory inventory, int position) {
        inventory.setItem(position, createItem(getColoredGlassPane(player), " "));
    }

    /**
     * Gets the colored glass pane material based on the player's settings.
     *
     * @param player The player to get the glass pane for
     * @return The colored glass pane material
     */
    public static XMaterial getColoredGlassPane(Player player) {
        Settings settings = plugin.getSettingsManager().getSettings(player);
        DesignColor color = settings.getDesignColor();

        switch (color) {
            case RED:
                return XMaterial.RED_STAINED_GLASS_PANE;
            case ORANGE:
                return XMaterial.ORANGE_STAINED_GLASS_PANE;
            case YELLOW:
                return XMaterial.YELLOW_STAINED_GLASS_PANE;
            case PINK:
                return XMaterial.PINK_STAINED_GLASS_PANE;
            case MAGENTA:
                return XMaterial.MAGENTA_STAINED_GLASS_PANE;
            case PURPLE:
                return XMaterial.PURPLE_STAINED_GLASS_PANE;
            case BROWN:
                return XMaterial.BROWN_STAINED_GLASS_PANE;
            case LIME:
                return XMaterial.LIME_STAINED_GLASS_PANE;
            case GREEN:
                return XMaterial.GREEN_STAINED_GLASS_PANE;
            case BLUE:
                return XMaterial.BLUE_STAINED_GLASS_PANE;
            case CYAN:
                return XMaterial.CYAN_STAINED_GLASS_PANE;
            case LIGHT_BLUE:
                return XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
            case WHITE:
                return XMaterial.WHITE_STAINED_GLASS_PANE;
            case GRAY:
                return XMaterial.GRAY_STAINED_GLASS_PANE;
            case LIGHT_GRAY:
                return XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE;
            case BLACK:
            default:
                return XMaterial.BLACK_STAINED_GLASS_PANE;
        }
    }

    /**
     * Adds a world item to the given inventory at the specified position.
     *
     * @param inventory   The inventory to add the item to
     * @param position    The position to add the item at
     * @param buildWorld  The world to create the item for
     * @param displayName The display name of the item
     * @param lore        The lore of the item
     */
    public static void addWorldItem(Inventory inventory, int position, BuildWorld buildWorld, String displayName, List<String> lore) {
        XMaterial material = buildWorld.getData().material().get();
        if (material != XMaterial.PLAYER_HEAD) {
            inventory.setItem(position, createItem(material, displayName, lore));
            return;
        }

        // Initially set a default head
        inventory.setItem(position, createItem(XMaterial.PLAYER_HEAD, displayName, lore));

        // Then try to set texture asynchronously
        XSkull.createItem()
                .profile(buildWorld.getData().privateWorld().get()
                        ? buildWorld.asProfilable()
                        : Profileable.username(buildWorld.getName())
                )
                .fallback(buildWorld.asProfilable())
                .lenient()
                .applyAsync()
                .thenAcceptAsync(itemStack -> {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta == null) {
                        return;
                    }
                    itemMeta.setDisplayName(displayName);
                    itemMeta.setLore(lore);
                    itemStack.setItemMeta(itemMeta);
                    inventory.setItem(position, itemStack);
                });
    }

    /**
     * Checks if a click event is valid for the given inventory title.
     *
     * @param event    The click event to check
     * @param titleKey The key of the inventory title
     * @return true if the click is valid, false otherwise
     */
    public static boolean isValidClick(InventoryClickEvent event, String titleKey) {
        String title = XInventoryView.of(event.getView()).getTitle();
        if (!title.equals(Messages.getString(titleKey, (Player) event.getWhoClicked()))) {
            return false;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return false;
        }

        event.setCancelled(true);
        return true;
    }

    /**
     * Checks if an {@link ItemStack} is a navigator item.
     *
     * @param player    The player to check for
     * @param itemStack The item to check
     * @return true if the item is a navigator, false otherwise
     */
    public static boolean isNavigator(Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != plugin.getConfigValues().getNavigatorItem().get()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }

        return itemMeta.getDisplayName().equals(Messages.getString("navigator_item", player));
    }

    /**
     * Gets all slots containing a navigator item in a player's inventory.
     *
     * @param player The player to check
     * @return A list of slot numbers containing navigator items
     */
    public static List<Integer> getNavigatorSlots(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        List<Integer> navigatorSlots = new ArrayList<>();

        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack currentItem = playerInventory.getItem(i);
            if (isNavigator(player, currentItem)) {
                navigatorSlots.add(i);
            }
        }

        return navigatorSlots;
    }

    /**
     * Checks if a player's inventory contains a navigator item.
     *
     * @param player The player to check
     * @return true if the inventory contains a navigator, false otherwise
     */
    public static boolean hasNavigator(Player player) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (isNavigator(player, itemStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces an {@link ItemStack} in a player's inventory with another {@link ItemStack}.
     *
     * @param player       The player whose inventory to modify
     * @param findItemName The name of the item to find
     * @param findItemType The type of the item to find
     * @param replaceItem  The item to replace with
     */
    public static void replaceItem(Player player, String findItemName, XMaterial findItemType, ItemStack replaceItem) {
        PlayerInventory inventory = player.getInventory();

        // First try to find the item to replace
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack currentItem = inventory.getItem(i);
            if (currentItem == null || currentItem.getType() != findItemType.get()) {
                continue;
            }

            ItemMeta itemMeta = currentItem.getItemMeta();
            if (itemMeta != null && itemMeta.getDisplayName().equals(findItemName)) {
                inventory.setItem(i, replaceItem);
                return;
            }
        }

        // If item not found, try to put in slot 8 or add to inventory
        ItemStack slot8 = inventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            inventory.setItem(8, replaceItem);
        } else {
            inventory.addItem(replaceItem);
        }
    }

    /**
     * Fills an {@link Inventory with glass panes.
     *
     * @param inventory   The inventory to fill
     * @param player      The player viewing the inventory
     * @param currentPage The current page number
     * @param numOfPages  The total number of pages
     */
    public static void fillWithGlass(Inventory inventory, Player player, int currentPage, int numOfPages) {
        for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++) {
            addGlassPane(player, inventory, i);
        }

        if (numOfPages > 1) {
            addGlassPane(player, inventory, inventory.getSize() - 5);
            ItemStack pageItem = inventory.getItem(inventory.getSize() - 5);
            ItemMeta meta = pageItem.getItemMeta();
            meta.setDisplayName(Messages.getString("gui_page", player,
                    new AbstractMap.SimpleEntry<>("page", String.valueOf(currentPage + 1)),
                    new AbstractMap.SimpleEntry<>("maxPage", String.valueOf(numOfPages))
            ));
            pageItem.setItemMeta(meta);
        }
    }
}