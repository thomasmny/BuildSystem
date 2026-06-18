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
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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
 * Builds and inspects stateful menu items. Holds the dependencies the item builders need (config, messages, settings)
 * so menus do not reach for the plugin singleton. Pure, stateless item construction lives in {@link ItemBuilder}.
 */
@NullMarked
public final class MenuItems {

    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final SettingsService settingsService;

    public final NamespacedKey navigatorKey;

    public MenuItems(
            JavaPlugin plugin, ConfigService configService, Messages messages, SettingsService settingsService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.settingsService = settingsService;
        this.navigatorKey = new NamespacedKey(plugin, "navigator");
    }

    /**
     * Adds a glass pane to the given inventory at the specified position.
     *
     * @param player The player viewing the inventory
     * @param inventory The inventory to add the glass pane to
     * @param position The position to add the glass pane at
     */
    public void addGlassPane(Player player, Inventory inventory, int position) {
        ItemBuilder.of(getColoredGlassPane(player)).name(" ").into(inventory, position);
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
     * Renders a {@link Displayable} into a menu slot. This is the single rendering path for every displayable: a
     * non-head icon or a configured {@link Displayable#getIconSkullTexture() skull texture} is applied synchronously,
     * while a displayable's {@link Displayable#getHeadProfile() default head profile} (e.g. a world's creator) is
     * resolved asynchronously so opening a menu never blocks on profile lookups.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot to add the item at
     * @param displayable The displayable to render
     * @param viewer The player viewing the inventory
     */
    public void renderDisplayable(Inventory inventory, int slot, Displayable displayable, Player viewer) {
        XMaterial icon = displayable.getIcon();
        String name = displayable.getDisplayName(viewer);
        List<String> lore = displayable.getLore(viewer);

        String texture = displayable.getIconSkullTexture();
        if (icon != XMaterial.PLAYER_HEAD || (texture != null && !texture.isBlank())) {
            ItemBuilder.icon(icon, texture, viewer).name(name).lore(lore).into(inventory, slot);
            return;
        }

        Profileable headProfile = displayable.getHeadProfile();
        if (headProfile == null) {
            ItemBuilder.of(XMaterial.PLAYER_HEAD).name(name).lore(lore).into(inventory, slot);
            return;
        }
        applyHeadProfileAsync(inventory, slot, headProfile, displayable.getHeadFallbackProfile(), name, lore);
    }

    private void applyHeadProfileAsync(
            Inventory inventory,
            int slot,
            Profileable profile,
            @Nullable Profileable fallback,
            String name,
            List<String> lore) {
        ItemStack placeholder =
                ItemBuilder.of(XMaterial.PLAYER_HEAD).name(name).lore(lore).build();
        inventory.setItem(slot, placeholder);

        XSkull.createItem()
                .profile(profile)
                .fallback(fallback != null ? new Profileable[] {fallback} : new Profileable[0])
                .lenient()
                .applyAsync()
                .thenAcceptAsync(itemStack -> {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta == null) {
                        return;
                    }
                    itemMeta.setDisplayName(name);
                    itemMeta.setLore(lore);
                    itemStack.setItemMeta(itemMeta);
                    Bukkit.getScheduler().runTask(plugin, () -> inventory.setItem(slot, itemStack));
                })
                .exceptionally(throwable -> {
                    plugin.getLogger()
                            .log(Level.WARNING, "Failed to resolve head profile for menu item: " + name, throwable);
                    return null;
                });
    }

    /**
     * Renders a {@link NavigatorCategory}'s icon into a menu slot, resolving any player-head skin asynchronously so the
     * live navigator never blocks the main thread on a profile lookup (an admin-configured username texture, or the
     * viewer's own head used by the {@code private}-style categories). Non-head icons render synchronously. Mirrors
     * {@link ItemBuilder#icon(NavigatorCategory, Player)}'s default-texture choice.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot to add the item at
     * @param category The category whose icon is rendered
     * @param viewer The player viewing the inventory
     * @param name The already-styled display name to apply
     * @param lore The lore to apply
     */
    public void renderCategoryIcon(
            Inventory inventory, int slot, NavigatorCategory category, Player viewer, String name, List<String> lore) {
        XMaterial icon = category.getIcon();
        String texture = ItemBuilder.categoryTexture(category);
        if (icon != XMaterial.PLAYER_HEAD || texture == null || texture.isBlank()) {
            ItemBuilder.icon(icon, texture, viewer).name(name).lore(lore).into(inventory, slot);
            return;
        }

        Profileable profile = ItemBuilder.VIEWER_HEAD.equals(texture)
                ? Profileable.detect(viewer.getName())
                : Profileable.detect(texture);
        applyHeadProfileAsync(inventory, slot, profile, null, name, lore);
    }

    /**
     * Checks if an {@link ItemStack} is a navigator item.
     *
     * @param itemStack The item to check
     * @return true if the item is a navigator, false otherwise
     */
    public boolean isNavigator(@Nullable ItemStack itemStack) {
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

    @Contract("_ -> new")
    public ItemStack createNavigatorItem(Player player) {
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
     * @param player The player whose inventory to modify
     * @param findItemName The name of the item to find
     * @param findItemType The type of the item to find
     * @param replaceItem The item to replace with
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

        ItemStack slot8 = inventory.getItem(8);
        if (slot8 == null || slot8.getType() == XMaterial.AIR.get()) {
            inventory.setItem(8, replaceItem);
        } else {
            inventory.addItem(replaceItem);
        }
    }

    /**
     * Adds a toggle item: a named item that glows (an unbreaking enchant) when enabled. Used by the editor and
     * player-settings menus for their on/off entries.
     *
     * @param player The player viewing the inventory
     * @param inventory The inventory to add the item to
     * @param slot The slot to place the item at
     * @param material The item material
     * @param enabled Whether the toggle is currently on (adds the glow)
     * @param displayNameKey The message key for the display name
     * @param loreKey The message key for the lore
     */
    public void addToggleItem(
            Player player,
            Inventory inventory,
            int slot,
            XMaterial material,
            boolean enabled,
            String displayNameKey,
            String loreKey) {
        List<String> lore = new ArrayList<>(messages.getStringList(loreKey, player));
        lore.add("");
        lore.add(messages.getString(enabled ? "toggle_currently_enabled" : "toggle_currently_disabled", player));
        ItemBuilder.of(material)
                .name(messages.getString(displayNameKey, player))
                .lore(lore)
                .glow(enabled)
                .into(inventory, slot);
    }

    /**
     * Fills the top and bottom rows of an {@link Inventory} with glass panes.
     *
     * @param inventory The inventory to fill
     * @param player The player viewing the inventory
     */
    public void fillWithGlass(Inventory inventory, Player player) {
        IntStream.rangeClosed(0, 8).forEach(i -> addGlassPane(player, inventory, i));
        IntStream.rangeClosed(45, 53).forEach(i -> addGlassPane(player, inventory, i));
    }

    /**
     * Fills every slot in the given range with glass panes.
     *
     * @param player The player viewing the inventory
     * @param inventory The inventory to fill
     * @param fromInclusive The first slot to fill (inclusive)
     * @param toExclusive The slot to stop before (exclusive)
     */
    public void fillRange(Player player, Inventory inventory, int fromInclusive, int toExclusive) {
        IntStream.range(fromInclusive, toExclusive).forEach(i -> addGlassPane(player, inventory, i));
    }

    /**
     * Fills the whole inventory with glass panes.
     *
     * @param player The player viewing the inventory
     * @param inventory The inventory to fill
     */
    public void fillAll(Player player, Inventory inventory) {
        fillRange(player, inventory, 0, inventory.getSize());
    }
}
