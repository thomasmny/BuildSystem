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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Shared editor scaffolding for a single styled, named registry entry (a world status or a navigator category). Holds
 * the property-button widgets common to both editors — rename, colour (via {@link DyePickerMenu}), icon (drag an item
 * onto the slot), boolean toggles, and back — plus the {@link #save(Player) save → reopen} cycle. Subclasses persist
 * through {@link #persist()} and lay out their own entry-specific buttons.
 */
@NullMarked
abstract class RegistryEditorMenu extends ButtonMenu<MenuButton> {

    protected final BuildSystemPlugin plugin;

    protected RegistryEditorMenu(BuildSystemPlugin plugin, String title) {
        super(plugin.getMessages(), 54, title);
        this.plugin = plugin;
    }

    /**
     * Persists the edited entry through its registry.
     */
    protected abstract void persist();

    /**
     * Re-opens this editor (a fresh instance) for the player, reflecting the latest state.
     *
     * @param player The viewing player
     */
    protected abstract void reopen(Player player);

    /**
     * Opens the management list this editor belongs to (the back target).
     *
     * @param player The viewing player
     */
    protected abstract void openManagement(Player player);

    /**
     * Persists the entry and re-opens the editor so the change is reflected immediately.
     *
     * @param player The viewing player
     */
    protected final void save(Player player) {
        persist();
        reopen(player);
    }

    protected final MenuButton renameButton(String nameKey, String promptKey, Consumer<String> apply) {
        return labelled(
                XMaterial.NAME_TAG,
                nameKey,
                (player, event) -> PlayerChatInput.requestSanitizedName(
                        plugin, player, promptKey, "setup_name_invalid_characters", "setup_name_empty", name -> {
                            apply.accept(name);
                            save(player);
                        }));
    }

    protected final MenuButton colorButton(String nameKey, Consumer<String> apply) {
        return labelled(
                XMaterial.INK_SAC,
                nameKey,
                (player, event) -> new DyePickerMenu(
                                plugin,
                                player,
                                token -> {
                                    apply.accept(token);
                                    save(player);
                                },
                                () -> reopen(player))
                        .open(player));
    }

    protected final MenuButton iconButton(String nameKey, Supplier<XMaterial> getter, Consumer<XMaterial> setter) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(getter.get())
                        .name(messages.getString(nameKey, player))
                        .lore(messages.getStringList("setup_icon_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> new MaterialPickerMenu(
                                plugin,
                                player,
                                material -> {
                                    setter.accept(material);
                                    save(player);
                                },
                                () -> reopen(player))
                        .open(player))
                .build();
    }

    protected final MenuButton toggleButton(String nameKey, BooleanSupplier state, Runnable toggle) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(
                                state.getAsBoolean() ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE)
                        .name(messages.getString(nameKey, player))
                        .lore(messages.getStringList(
                                state.getAsBoolean() ? "setup_toggle_enabled" : "setup_toggle_disabled", player))
                        .glow(state.getAsBoolean())
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    toggle.run();
                    save(player);
                })
                .build();
    }

    protected final MenuButton labelled(XMaterial icon, String nameKey, MenuButton.ClickHandler onClick) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(icon)
                        .name(messages.getString(nameKey, player))
                        .into(inventory, slot))
                .onClick(onClick)
                .build();
    }

    protected final MenuButton backButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.BARRIER)
                        .name(messages.getString("setup_back", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    openManagement(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillAll(player, getInventory());
        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Filler clicks do nothing; navigation is via the explicit back button.
    }
}
