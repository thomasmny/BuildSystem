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
package de.eintosti.buildsystem.util.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Displayable.DisplayableType;
import de.eintosti.buildsystem.config.Config.Settings.Navigator;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for inventory-related operations. All methods are static and the class cannot be instantiated.
 */
public final class InventoryUtils {

    private static final BuildSystemPlugin PLUGIN = BuildSystemPlugin.getPlugin(BuildSystemPlugin.class);
    private static final Logger LOGGER = PLUGIN.getLogger();

    public final static NamespacedKey DISPLAYABLE_NAME_KEY = new NamespacedKey(PLUGIN, "displayable_name");
    public final static NamespacedKey DISPLAYABLE_TYPE_KEY = new NamespacedKey(PLUGIN, "displayable_type");

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
    public static ItemStack createItem(@NotNull XMaterial material, @NotNull String displayName, @NotNull List<String> lore) {
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
        Settings settings = PLUGIN.getSettingsManager().getSettings(player);
        DesignColor color = settings.getDesignColor();
        String paneItemName = color.name() + "_STAINED_GLASS_PANE";
        return XMaterial.matchXMaterial(paneItemName).orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
    }

    /**
     * Adds a world item to the given inventory at the specified slot.
     *
     * @param inventory   The inventory to add the item to
     * @param slot        The slot to add the item at
     * @param buildWorld  The world to create the item for
     * @param displayName The display name of the item
     * @param lore        The lore of the item
     */
    public static void addWorldItem(Inventory inventory, int slot, BuildWorld buildWorld, String displayName, List<String> lore) {
        XMaterial material = buildWorld.getData().material().get();
        if (material != XMaterial.PLAYER_HEAD) {
            inventory.setItem(slot, createItem(material, displayName, lore));
            return;
        }

        // Initially set a default head
        ItemStack defaultHead = createItem(XMaterial.PLAYER_HEAD, displayName, lore);
        storeWorldInformation(defaultHead, buildWorld);
        inventory.setItem(slot, defaultHead);

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
                    itemMeta.setDisplayName(displayName);
                    itemMeta.setLore(lore);
                    itemStack.setItemMeta(itemMeta);
                    storeWorldInformation(itemStack, buildWorld);
                    inventory.setItem(slot, itemStack);
                });
    }

    /**
     * Stores information about the given {@link BuildWorld} in the given item's {@link PersistentDataContainer}.
     *
     * @param itemStack  The item stack to store the world information in
     * @param buildWorld The world to store information about
     */
    private static void storeWorldInformation(@NotNull ItemStack itemStack, BuildWorld buildWorld) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(DISPLAYABLE_TYPE_KEY, PersistentDataType.STRING, DisplayableType.BUILD_WORLD.name());
        pdc.set(DISPLAYABLE_NAME_KEY, PersistentDataType.STRING, buildWorld.getName());
        itemStack.setItemMeta(itemMeta);
    }

    /**
     * Checks if an {@link ItemStack} is a navigator item.
     *
     * @param player    The player to check for
     * @param itemStack The item to check
     * @return true if the item is a navigator, false otherwise
     */
    public static boolean isNavigator(Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Navigator.item.get()) {
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
        return IntStream.range(0, playerInventory.getSize())
                .filter(i -> isNavigator(player, playerInventory.getItem(i)))
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Checks if a player's inventory contains a navigator item.
     *
     * @param player The player to check
     * @return true if the inventory contains a navigator, false otherwise
     */
    public static boolean hasNavigator(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .anyMatch(itemStack -> isNavigator(player, itemStack));
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

        // If item not found, try to put in slot 8 or add to inventory
        ItemStack slot8 = inventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            inventory.setItem(8, replaceItem);
        } else {
            inventory.addItem(replaceItem);
        }
    }

    /**
     * Fills the top and bottom rows of an {@link Inventory} with glass panes.
     *
     * @param inventory The inventory to fill
     * @param player    The player viewing the inventory
     */
    public static void fillWithGlass(Inventory inventory, Player player) {
        IntStream.rangeClosed(0, 8).forEach(i -> addGlassPane(player, inventory, i));
        IntStream.rangeClosed(45, 53).forEach(i -> addGlassPane(player, inventory, i));
    }
}