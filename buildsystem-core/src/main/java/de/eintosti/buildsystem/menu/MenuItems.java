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
import com.cryptomorin.xseries.profiles.exceptions.ProfileException;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Builds and inspects stateful menu items. Holds the dependencies the item builders need (config, messages, settings)
 * so menus do not reach for the plugin singleton. Pure, stateless item construction lives in {@link ItemBuilder}.
 */
@NullMarked
public final class MenuItems {

    private final JavaPlugin plugin;
    private final Messages messages;
    private final SettingsService settingsService;

    public MenuItems(JavaPlugin plugin, Messages messages, SettingsService settingsService) {
        this.plugin = plugin;
        this.messages = messages;
        this.settingsService = settingsService;
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
     * Renders an icon into a menu slot. A non-head icon is applied synchronously; a player head is resolved
     * asynchronously — the slot shows a plain head until the profile arrives — because resolving a username or UUID
     * profile talks to Mojang and would otherwise freeze the server thread. A configured {@code texture} wins over
     * {@code defaultProfile}; when neither is present the slot keeps its plain head.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot to add the item at
     * @param icon The icon material
     * @param texture The configured skull texture, the {@link ItemBuilder#VIEWER_HEAD} sentinel, or {@code null}
     * @param defaultProfile The head profile used when no texture is configured, or {@code null} for none
     * @param fallback The profile to fall back to if the chosen one cannot be resolved, or {@code null} for none
     * @param viewer The player viewing the inventory
     * @param name The already-styled display name to apply
     * @param lore The lore to apply
     */
    public void renderIcon(
            Inventory inventory,
            int slot,
            XMaterial icon,
            @Nullable String texture,
            @Nullable Profileable defaultProfile,
            @Nullable Profileable fallback,
            Player viewer,
            String name,
            List<String> lore) {
        if (icon != XMaterial.PLAYER_HEAD) {
            ItemBuilder.of(icon).name(name).lore(lore).into(inventory, slot);
            return;
        }

        if (texture != null && !texture.isBlank() && decodesLocally(texture)) {
            ItemBuilder.skull(Profileable.detect(texture)).name(name).lore(lore).into(inventory, slot);
            return;
        }

        Profileable profile = texture != null && !texture.isBlank() ? profileFor(texture, viewer) : defaultProfile;
        if (profile == null) {
            ItemBuilder.of(XMaterial.PLAYER_HEAD).name(name).lore(lore).into(inventory, slot);
            return;
        }
        applyHeadProfileAsync(inventory, slot, profile, fallback, name, lore, null);
    }

    /**
     * {@return whether a configured skull texture carries the skin itself} A base64 blob, a texture URL or a bare
     * texture hash decodes locally in microseconds and is applied straight away — only the inputs that name a player
     * (a username, a UUID, the {@link ItemBuilder#VIEWER_HEAD} sentinel) need the asynchronous path, and sending those
     * through it too would make every static menu icon flicker for a tick.
     */
    private static boolean decodesLocally(String texture) {
        ProfileInputType type = ProfileInputType.typeOf(texture);
        return type == ProfileInputType.BASE64
                || type == ProfileInputType.TEXTURE_URL
                || type == ProfileInputType.TEXTURE_HASH;
    }

    /**
     * {@return the profile a configured skull texture refers to} The {@link ItemBuilder#VIEWER_HEAD} sentinel resolves
     * to the viewing player, anything else is detected from the texture string itself.
     */
    public static Profileable profileFor(String texture, Player viewer) {
        return Profileable.detect(ItemBuilder.VIEWER_HEAD.equals(texture) ? viewer.getName() : texture);
    }

    /**
     * Renders a {@link Displayable} into a menu slot using its own name and lore.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot to add the item at
     * @param displayable The displayable to render
     * @param viewer The player viewing the inventory
     */
    public void renderDisplayable(Inventory inventory, int slot, Displayable displayable, Player viewer) {
        renderDisplayable(
                inventory, slot, displayable, viewer, displayable.getDisplayName(viewer), displayable.getLore(viewer));
    }

    /**
     * Variant of {@link #renderDisplayable(Inventory, int, Displayable, Player)} for callers that label the item
     * themselves (the world editor's icon button). The icon, its configured texture and — for an untextured head — the
     * {@link HeadProfileSource} default profile (e.g. a world's creator) still come from the displayable.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot to add the item at
     * @param displayable The displayable whose icon is rendered
     * @param viewer The player viewing the inventory
     * @param name The already-styled display name to apply
     * @param lore The lore to apply
     */
    public void renderDisplayable(
            Inventory inventory, int slot, Displayable displayable, Player viewer, String name, List<String> lore) {
        HeadProfileSource source = displayable instanceof HeadProfileSource head ? head : null;
        renderIcon(
                inventory,
                slot,
                XMaterial.matchXMaterial(displayable.getIcon()),
                displayable.getIconSkullTexture(),
                source != null ? source.getHeadProfile() : null,
                source != null ? source.getHeadFallbackProfile() : null,
                viewer,
                name,
                lore);
    }

    /**
     * Renders a {@link NavigatorCategory}'s icon into a menu slot, applying the texture chosen by
     * {@link ItemBuilder#categoryTexture(NavigatorCategory)}.
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
        renderIcon(
                inventory,
                slot,
                XMaterial.matchXMaterial(category.getIcon()),
                ItemBuilder.categoryTexture(category),
                null,
                null,
                viewer,
                name,
                lore);
    }

    /**
     * Renders a placeholder head into a menu slot immediately, then resolves the given head profile asynchronously and
     * swaps in the finished stack on the main thread once it's ready. The swap is skipped when the slot no longer holds
     * the placeholder: the layout editor renders into the player's own inventory, which is restored when the editor
     * closes, and clobbering a restored item would lose it.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot to add the item at
     * @param profile The head profile to resolve
     * @param fallback The profile to fall back to if {@code profile} cannot be resolved, or {@code null} for none
     * @param name The already-styled display name to apply
     * @param lore The lore to apply
     * @param finisher Applied to both the placeholder and the resolved stack, for decoration the swap must not drop
     *     (e.g. persistent data a click handler reads back); may be {@code null}
     */
    public void applyHeadProfileAsync(
            Inventory inventory,
            int slot,
            Profileable profile,
            @Nullable Profileable fallback,
            String name,
            List<String> lore,
            @Nullable Consumer<ItemStack> finisher) {
        ItemStack placeholder =
                ItemBuilder.of(XMaterial.PLAYER_HEAD).name(name).lore(lore).build();
        if (finisher != null) {
            finisher.accept(placeholder);
        }
        inventory.setItem(slot, placeholder);

        resolveHeadAsync(profile, fallback, name, lore, itemStack -> {
            if (finisher != null) {
                finisher.accept(itemStack);
            }
            if (placeholder.isSimilar(inventory.getItem(slot))) {
                inventory.setItem(slot, itemStack);
            }
        });
    }

    /**
     * Resolves a head profile off the main thread and hands the finished, named and lored stack to {@code onResolved}
     * back on the main thread; nothing is called when the profile cannot be resolved. For inventory slots use
     * {@link #applyHeadProfileAsync}, which also places a placeholder — this is the primitive for the heads that are
     * not menu items, such as an armour stand's helmet or an item handed straight to a player.
     *
     * @param profile The head profile to resolve
     * @param fallback The profile to fall back to if {@code profile} cannot be resolved, or {@code null} for none
     * @param name The already-styled display name to apply
     * @param lore The lore to apply
     * @param onResolved Receives the finished stack on the main thread
     */
    public void resolveHeadAsync(
            Profileable profile,
            @Nullable Profileable fallback,
            String name,
            List<String> lore,
            Consumer<ItemStack> onResolved) {
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
                    Bukkit.getScheduler().runTask(plugin, () -> onResolved.accept(itemStack));
                })
                .exceptionally(throwable -> {
                    logProfileFailure(name, throwable);
                    return null;
                });
    }

    /**
     * A head that Mojang does not know — an offline-mode UUID, a renamed account, a world name that is not a player —
     * is routine: the slot keeps its plain placeholder head. Only unexpected failures (network, reflection) warrant a
     * warning with a stack trace.
     */
    private void logProfileFailure(String name, Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        if (cause instanceof ProfileException || cause.getCause() instanceof ProfileException) {
            plugin.getLogger()
                    .log(Level.FINE, () -> "No head profile for menu item " + name + ": " + cause.getMessage());
            return;
        }
        plugin.getLogger().log(Level.WARNING, "Failed to resolve head profile for menu item: " + name, throwable);
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
