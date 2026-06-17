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

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Fluent builder for the {@link ItemStack}s shown in menus.
 *
 * <p>Construction starts with a factory method that fixes the item's base type — {@link #of(XMaterial)} for a regular
 * material, {@link #skull(Profileable)} for a textured player head, or {@link #glassPane(Player, SettingsService)} for a
 * filler pane tinted to the player's chosen design colour. From there, chainable mutators ({@link #name(String)},
 * {@link #lore(List)}, {@link #glow(boolean)}, {@link #pdc}) decorate the item, and a terminal call
 * ({@link #build()} or {@link #into(Inventory, int)}) materialises it.
 *
 * <p><strong>Default flags.</strong> {@link #of(XMaterial)} hides all {@link ItemFlag}s, because menu items are
 * decorative and their vanilla attribute/enchant tooltips would be noise. {@link #skull(Profileable)} does <em>not</em>
 * hide flags, preserving the previous skull-rendering behaviour.
 *
 * <p><strong>Mutability and reuse.</strong> An instance wraps a single {@link ItemStack} and is <em>not</em> reusable:
 * each factory call produces a fresh builder, and the terminal {@code build()}/{@code into(...)} returns/places that one
 * stack. The builder is not thread-safe and is intended to be used on the main thread, chained in a single expression.
 *
 * <p>Mutators tolerate a missing {@link ItemMeta} (some exotic materials have none): when meta is unavailable the
 * decorating call is silently skipped rather than throwing.
 */
@NullMarked
public final class ItemBuilder {

    private static final Logger LOGGER = Logger.getLogger(ItemBuilder.class.getName());

    /**
     * Sentinel skull-texture value meaning "render the viewing player's own head" rather than a fixed texture.
     */
    public static final String VIEWER_HEAD = "%viewer%";

    private final ItemStack itemStack;
    private @Nullable ItemMeta itemMeta;

    private ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * Starts a builder for a regular item, with all {@link ItemFlag}s hidden (see the class note on default flags). If
     * the material cannot be resolved to a Bukkit item it falls back to {@link XMaterial#BEDROCK} and logs a warning,
     * so callers always receive a usable builder.
     *
     * @param material The material of the item
     * @return A new builder wrapping the resolved item
     */
    public static ItemBuilder of(XMaterial material) {
        ItemStack base = material.parseItem();
        if (base == null) {
            base = XMaterial.BEDROCK.parseItem();
            LOGGER.warning("Unknown material (%s). Defaulting to BEDROCK.".formatted(material.name()));
        }
        assert base != null;
        return new ItemBuilder(base).hideAttributes();
    }

    /**
     * Starts a builder for a textured player head. Unlike {@link #of(XMaterial)} this does not hide item flags, matching
     * the historical skull-rendering behaviour. Profile resolution is lenient: an unresolvable profile yields a plain
     * head rather than throwing.
     *
     * @param profileable The skull profile (player UUID, name, or texture) to apply
     * @return A new builder wrapping the head
     */
    public static ItemBuilder skull(Profileable profileable) {
        ItemStack skull = XSkull.createItem().profile(profileable).lenient().apply();
        return new ItemBuilder(skull);
    }

    /**
     * Starts a builder for a configurable icon. When {@code material} is a player head, the head's skin is resolved from
     * {@code skullTexture}: the {@link #VIEWER_HEAD} sentinel uses the viewing player's own skin, a non-blank value is
     * treated as a texture/name/UUID profile, and {@code null}/blank yields a plain head. For any other material this is
     * equivalent to {@link #of(XMaterial)}.
     *
     * @param material The base icon material
     * @param skullTexture The skull texture to apply when {@code material} is a head, or {@code null}
     * @param viewer The viewing player, used to resolve the {@link #VIEWER_HEAD} sentinel; may be {@code null}
     * @return A new builder wrapping the resolved icon
     */
    public static ItemBuilder icon(XMaterial material, @Nullable String skullTexture, @Nullable Player viewer) {
        if (material != XMaterial.PLAYER_HEAD) {
            return of(material);
        }
        if (VIEWER_HEAD.equals(skullTexture) && viewer != null) {
            return skull(Profileable.detect(viewer.getName()));
        }
        if (skullTexture != null && !skullTexture.isBlank()) {
            return skull(Profileable.detect(skullTexture));
        }
        return of(XMaterial.PLAYER_HEAD);
    }

    /**
     * Starts a builder for a {@link NavigatorCategory navigator category} icon. An explicitly configured skull texture
     * wins; otherwise a player-head icon defaults to the viewing player's own skin for added-players categories (the
     * "private" style) and the navigator texture for everyone-visible categories.
     *
     * @param category The category whose icon is rendered
     * @param viewer The viewing player, used to resolve the viewer-head default
     * @return A new builder wrapping the resolved icon
     */
    public static ItemBuilder icon(NavigatorCategory category, Player viewer) {
        String texture = category.getIconSkullTexture();
        if (category.getIcon() == XMaterial.PLAYER_HEAD && (texture == null || texture.isBlank())) {
            texture = category.getPrimaryVisibility() == Visibility.ADDED_PLAYERS
                    ? VIEWER_HEAD
                    : SkullTextures.WORLD_NAVIGATOR;
        }
        return icon(category.getIcon(), texture, viewer);
    }

    /**
     * Resolves a {@link Displayable}'s icon to a builder <em>without</em> applying its name or lore, so the caller can
     * label it itself: a non-head icon or a configured skull texture is honoured, otherwise the displayable's
     * {@link Displayable#getHeadProfile() default head profile} is applied. This is the synchronous counterpart to
     * {@code MenuItems.renderDisplayable} and is intended for single items, not bulk lists.
     *
     * @param displayable The displayable whose icon is rendered
     * @param viewer The viewing player, used to resolve the viewer-head sentinel
     * @return A new builder wrapping the resolved icon
     */
    public static ItemBuilder icon(Displayable displayable, Player viewer) {
        XMaterial material = displayable.getIcon();
        String texture = displayable.getIconSkullTexture();
        if (material != XMaterial.PLAYER_HEAD || (texture != null && !texture.isBlank())) {
            return icon(material, texture, viewer);
        }
        Profileable headProfile = displayable.getHeadProfile();
        return headProfile != null ? skull(headProfile) : of(XMaterial.PLAYER_HEAD);
    }

    /**
     * Starts a builder for a filler glass pane tinted to the player's configured {@link DesignColor}, falling back to
     * black when the colour has no matching stained-glass-pane material. The pane is given a single-space name so it
     * renders without a visible label.
     *
     * @param player The player whose design colour selects the pane tint
     * @param settingsService The service used to read the player's settings
     * @return A new builder wrapping the tinted pane
     */
    public static ItemBuilder glassPane(Player player, SettingsService settingsService) {
        Settings settings = settingsService.getSettings(player);
        DesignColor color = settings.getDesignColor();
        XMaterial material = XMaterial.matchXMaterial(color.name() + "_STAINED_GLASS_PANE")
                .orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
        return of(material).name("§0");
    }

    /**
     * Sets the item's display name.
     *
     * @param displayName The display name (legacy colour codes supported)
     * @return This builder, for chaining
     */
    public ItemBuilder name(String displayName) {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
        }
        return this;
    }

    /**
     * Sets the item's lore, replacing any existing lore. The list is defensively copied.
     *
     * @param lines The lore lines, one per row
     * @return This builder, for chaining
     */
    public ItemBuilder lore(List<String> lines) {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.setLore(new ArrayList<>(lines));
        }
        return this;
    }

    /**
     * Varargs overload of {@link #lore(List)}.
     *
     * @param lines The lore lines, one per row
     * @return This builder, for chaining
     */
    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    private ItemBuilder hideAttributes() {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    /**
     * Toggles the enchantment-glow effect. When {@code enabled}, adds a hidden level-1 {@code UNBREAKING} enchantment so
     * the item shimmers without showing an enchantment tooltip; when {@code false} this is a no-op, which lets callers
     * pass a condition (e.g. "is this the selected entry?") directly.
     *
     * @param enabled Whether the item should glow
     * @return This builder, for chaining
     */
    public ItemBuilder glow(boolean enabled) {
        if (!enabled) {
            return this;
        }

        ensureMeta();
        if (itemMeta != null) {
            Enchantment enchantment = XEnchantment.UNBREAKING.get();
            if (enchantment != null) {
                itemStack.addUnsafeEnchantment(enchantment, 1);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        return this;
    }

    /**
     * Stores a value in the item's persistent data container, used to tag menu items (e.g. marking the navigator item)
     * so they can be recognised later.
     *
     * @param key The namespaced key to store under
     * @param type The persistent data type of the value
     * @param value The value to store
     * @param <T> The primitive storage type
     * @param <Z> The complex (runtime) value type
     * @return This builder, for chaining
     */
    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(key, type, value);
        }
        return this;
    }

    /**
     * Finalises the item: applies the accumulated {@link ItemMeta} and returns the backing {@link ItemStack}. The
     * builder should not be reused after this call.
     *
     * @return The built item
     */
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    /**
     * Builds the item and places it directly into the given inventory slot. Terminal convenience for the common
     * {@code inventory.setItem(slot, builder.build())} pattern.
     *
     * @param inventory The inventory to place the item into
     * @param slot The slot to place the item at
     */
    public void into(Inventory inventory, int slot) {
        inventory.setItem(slot, build());
    }

    private void ensureMeta() {
        if (itemMeta == null) {
            itemMeta = itemStack.getItemMeta();
        }
    }
}
