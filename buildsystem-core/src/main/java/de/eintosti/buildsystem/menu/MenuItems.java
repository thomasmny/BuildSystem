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
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Displayable.DisplayableType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Builds and inspects stateful menu items. Holds the dependencies the item builders need (config, messages, settings) so menus do not reach for the plugin singleton. Pure,
 * stateless item construction lives in {@link InventoryUtils}.
 */
@NullMarked
public final class MenuItems {

    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final SettingsService settingsService;

    public final NamespacedKey displayableNameKey;
    public final NamespacedKey displayableTypeKey;
    public final NamespacedKey navigatorKey;

    public MenuItems(JavaPlugin plugin, ConfigService configService, Messages messages, SettingsService settingsService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.settingsService = settingsService;
        this.displayableNameKey = new NamespacedKey(plugin, "displayable_name");
        this.displayableTypeKey = new NamespacedKey(plugin, "displayable_type");
        this.navigatorKey = new NamespacedKey(plugin, "navigator");
    }

    /**
     * Adds a glass pane to the given inventory at the specified position.
     *
     * @param player    The player viewing the inventory
     * @param inventory The inventory to add the glass pane to
     * @param position  The position to add the glass pane at
     */
    public void addGlassPane(Player player, Inventory inventory, int position) {
        inventory.setItem(position, InventoryUtils.createItem(getColoredGlassPane(player), " "));
    }

    /**
     * Gets the colored glass pane material based on the player's settings.
     *
     * @param player The player to get the glass pane for
     * @return The colored glass pane material
     */
    public XMaterial getColoredGlassPane(Player player) {
        Settings settings = settingsService.getSettings(player);
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
    public void addWorldItem(Inventory inventory, int slot, BuildWorld buildWorld, String displayName, List<String> lore) {
        XMaterial material = buildWorld.getData().material().get();
        if (material != XMaterial.PLAYER_HEAD) {
            inventory.setItem(slot, InventoryUtils.createItem(material, displayName, lore));
            return;
        }

        // Initially set a default head
        ItemStack defaultHead = InventoryUtils.createItem(XMaterial.PLAYER_HEAD, displayName, lore);
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
                    Bukkit.getScheduler().runTask(plugin, () -> inventory.setItem(slot, itemStack));
                });
    }

    private void storeWorldInformation(ItemStack itemStack, BuildWorld buildWorld) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(displayableTypeKey, PersistentDataType.STRING, DisplayableType.BUILD_WORLD.name());
        pdc.set(displayableNameKey, PersistentDataType.STRING, buildWorld.getName());
        itemStack.setItemMeta(itemMeta);
    }

    /**
     * Checks if an {@link ItemStack} is a navigator item.
     *
     * @param itemStack The item to check
     * @return true if the item is a navigator, false otherwise
     */
    public boolean isNavigator(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != configService.current().settings().navigator().item().get()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }

        return Boolean.TRUE.equals(itemStack.getItemMeta().getPersistentDataContainer().get(navigatorKey, PersistentDataType.BOOLEAN));
    }

    @Contract("_ -> new")
    public ItemStack createNavigatorItem(Player player) {
        ItemStack itemStack = InventoryUtils.createItem(configService.current().settings().navigator().item(), messages.getString("navigator_item", player));
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        itemMeta.getPersistentDataContainer().set(navigatorKey, PersistentDataType.BOOLEAN, true);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * Gets all slots containing a navigator item in a player's inventory.
     *
     * @param player The player to check
     * @return A list of slot numbers containing navigator items
     */
    @Unmodifiable
    public List<Integer> getNavigatorSlots(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        return IntStream.range(0, playerInventory.getSize())
                .filter(i -> isNavigator(playerInventory.getItem(i)))
                .boxed()
                .toList();
    }

    /**
     * Checks if a player's inventory contains a navigator item.
     *
     * @param player The player to check
     * @return true if the inventory contains a navigator, false otherwise
     */
    public boolean hasNavigator(Player player) {
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (isNavigator(itemStack)) {
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
    public void replaceItem(Player player, String findItemName, XMaterial findItemType, ItemStack replaceItem) {
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
    public void fillWithGlass(Inventory inventory, Player player) {
        IntStream.rangeClosed(0, 8).forEach(i -> addGlassPane(player, inventory, i));
        IntStream.rangeClosed(45, 53).forEach(i -> addGlassPane(player, inventory, i));
    }

    /**
     * Fills every slot in the given range with glass panes.
     *
     * @param player        The player viewing the inventory
     * @param inventory     The inventory to fill
     * @param fromInclusive The first slot to fill (inclusive)
     * @param toExclusive   The slot to stop before (exclusive)
     */
    public void fillRange(Player player, Inventory inventory, int fromInclusive, int toExclusive) {
        IntStream.range(fromInclusive, toExclusive).forEach(i -> addGlassPane(player, inventory, i));
    }

    /**
     * Fills the whole inventory with glass panes.
     *
     * @param player    The player viewing the inventory
     * @param inventory The inventory to fill
     */
    public void fillAll(Player player, Inventory inventory) {
        fillRange(player, inventory, 0, inventory.getSize());
    }
}
