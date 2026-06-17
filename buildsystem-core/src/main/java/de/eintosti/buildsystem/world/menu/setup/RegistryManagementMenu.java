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
package de.eintosti.buildsystem.world.menu.setup;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.world.menu.SetupMenu;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Shared paginated list for managing a registry of styled, named entries (world statuses or navigator categories): each
 * entry can be clicked to edit or shift-clicked to delete (custom entries only), and an add button creates a new entry
 * from a typed name. The layout, paging, add/delete/back flow, and built-in protection live here; subclasses supply only
 * the registry-specific bits.
 *
 * @param <T> The managed entry type
 */
@NullMarked
abstract class RegistryManagementMenu<T> extends PaginatedMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int FIRST_CONTENT_SLOT = 9;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_RESET = 47;
    private static final int SLOT_ADD = 49;
    private static final int SLOT_PREVIOUS_PAGE = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    protected final BuildSystemPlugin plugin;

    protected RegistryManagementMenu(BuildSystemPlugin plugin, Player player, String titleKey) {
        super(plugin.getMessages(), 54, plugin.getMessages().getString(titleKey, player));
        this.plugin = plugin;
    }

    @Override
    protected int totalItems() {
        return entries().size();
    }

    @Override
    protected void populate(Player player) {
        clearButtons();
        plugin.getMenuItems().fillWithGlass(getInventory(), player);

        registerPageItems(FIRST_CONTENT_SLOT, ITEMS_PER_PAGE, List.copyOf(entries()), this::entryButton);
        register(SLOT_BACK, backButton());
        register(SLOT_RESET, resetButton());
        register(SLOT_ADD, addButton());
        register(SLOT_PREVIOUS_PAGE, previousPageButton(SkullTextures.PREVIOUS_PAGE, ITEMS_PER_PAGE));
        register(SLOT_NEXT_PAGE, nextPageButton(SkullTextures.NEXT_PAGE, ITEMS_PER_PAGE));

        renderButtons(player);
    }

    private MenuButton entryButton(T entry) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> icon(entry, player)
                        .name(displayName(entry))
                        .lore(messages.getStringList(isBuiltIn(entry) ? builtInLoreKey() : entryLoreKey(), player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    if (event.isShiftClick()) {
                        promptDelete(player, entry);
                        return;
                    }
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    openEditor(player, entry);
                })
                .build();
    }

    private void promptDelete(Player player, T entry) {
        if (entries().size() <= 1) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            messages.sendMessage(player, "setup_delete_last");
            return;
        }
        new DeletionConfirmMenu(
                        plugin,
                        player,
                        displayName(entry),
                        confirmLore(entry, player),
                        () -> {
                            performDelete(entry);
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            reopen(player);
                        },
                        () -> reopen(player))
                .open(player);
    }

    private MenuButton resetButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.NETHER_STAR)
                        .name(messages.getString("setup_reset", player))
                        .lore(messages.getStringList("setup_reset_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> new DeletionConfirmMenu(
                                plugin,
                                player,
                                messages.getString("setup_reset", player),
                                messages.getStringList("setup_reset_confirm_lore", player),
                                () -> {
                                    performReset();
                                    XSound.ENTITY_CHICKEN_EGG.play(player);
                                    reopen(player);
                                },
                                () -> reopen(player))
                        .open(player))
                .build();
    }

    private MenuButton addButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                        .name(messages.getString(addNameKey(), player))
                        .into(inventory, slot))
                .onClick((player, event) -> PlayerChatInput.requestSanitizedName(
                        plugin,
                        player,
                        addPromptKey(),
                        "setup_name_invalid_characters",
                        "setup_name_empty",
                        name -> openEditor(player, create(name))))
                .build();
    }

    private MenuButton backButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.BARRIER)
                        .name(messages.getString("setup_back", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    new SetupMenu(plugin, player).open(player);
                })
                .build();
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Clicks on filler/empty slots are ignored; navigation is via the explicit buttons.
    }

    /**
     * {@return the entries to list}
     */
    protected abstract List<T> entries();

    /**
     * {@return an icon builder (material and any skull texture) for the entry, without name or lore}
     */
    protected abstract ItemBuilder icon(T entry, Player player);

    /**
     * {@return the coloured display name shown for the entry}
     */
    protected abstract String displayName(T entry);

    /**
     * {@return whether the entry is one of the seeded built-ins} Built-ins are still deletable, but are labelled
     * differently and are what {@link #performReset()} restores.
     */
    protected abstract boolean isBuiltIn(T entry);

    /**
     * Opens the editor for an entry.
     *
     * @param player The viewing player
     * @param entry The entry to edit
     */
    protected abstract void openEditor(Player player, T entry);

    /**
     * Creates a new entry from a typed name.
     *
     * @param name The sanitized name
     * @return The created entry
     */
    protected abstract T create(String name);

    /**
     * Deletes the entry. Never called for the last remaining entry.
     *
     * @param entry The entry to delete
     */
    protected abstract void performDelete(T entry);

    /**
     * Restores the seeded built-in entries, discarding customizations.
     */
    protected abstract void performReset();

    /**
     * Re-opens this management menu for the player after a create/delete.
     *
     * @param player The viewing player
     */
    protected abstract void reopen(Player player);

    /**
     * {@return the lore shown in the deletion confirmation for the entry}
     */
    protected abstract List<String> confirmLore(T entry, Player player);

    protected abstract String entryLoreKey();

    protected abstract String builtInLoreKey();

    protected abstract String addNameKey();

    protected abstract String addPromptKey();
}
