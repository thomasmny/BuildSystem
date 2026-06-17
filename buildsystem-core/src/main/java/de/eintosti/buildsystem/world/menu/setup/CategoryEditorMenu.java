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
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Edits a single {@link NavigatorCategory}: name, colour, icon (material or skull texture), the visibilities and member
 * statuses it groups, and its navigator placement (shown + slot). Built-in categories can be restyled here but never
 * deleted.
 */
@NullMarked
public class CategoryEditorMenu extends RegistryEditorMenu {

    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_RENAME = 19;
    private static final int SLOT_COLOR = 20;
    private static final int SLOT_ICON = 21;
    private static final int SLOT_SKULL = 22;
    private static final int SLOT_EVERYONE = 23;
    private static final int SLOT_ADDED_PLAYERS = 24;
    private static final int SLOT_STATUSES = 25;
    private static final int SLOT_SHOWN = 30;
    private static final int SLOT_NAV_SLOT = 32;
    private static final int SLOT_BACK = 49;

    private final NavigatorCategoryRegistryImpl registry;
    private final NavigatorCategoryImpl category;

    public CategoryEditorMenu(BuildSystemPlugin plugin, Player player, NavigatorCategory category) {
        super(
                plugin,
                plugin.getMessages()
                        .getString(
                                "setup_category_editor_title",
                                player,
                                Map.entry(
                                        "%category%",
                                        ColorAPI.process(category.getColor() + category.getDisplayName()))));
        this.registry = plugin.getNavigatorCategoryRegistry();
        this.category = (NavigatorCategoryImpl) category;

        register(SLOT_PREVIEW, previewButton());
        register(
                SLOT_RENAME,
                renameButton("setup_category_rename", "setup_category_rename_prompt", this.category::setDisplayName));
        register(SLOT_COLOR, colorButton("setup_category_color", this.category::setColor));
        register(SLOT_ICON, iconButton("setup_category_icon", this.category::getIcon, this.category::setIcon));
        register(SLOT_SKULL, skullButton());
        register(SLOT_EVERYONE, visibilityButton(Visibility.EVERYONE, "setup_category_visibility_everyone"));
        register(SLOT_ADDED_PLAYERS, visibilityButton(Visibility.ADDED_PLAYERS, "setup_category_visibility_added"));
        register(
                SLOT_STATUSES,
                labelled(
                        XMaterial.NAME_TAG,
                        "setup_category_statuses",
                        (p, event) -> new CategoryStatusesMenu(plugin, p, this.category).open(p)));
        register(
                SLOT_SHOWN,
                toggleButton(
                        "setup_category_shown",
                        this.category::isShownInNavigator,
                        () -> this.category.setShownInNavigator(!this.category.isShownInNavigator())));
        register(SLOT_NAV_SLOT, navigatorSlotButton());
        register(SLOT_BACK, backButton());
    }

    private MenuButton previewButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.icon(category, player)
                        .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                        .lore(messages.getStringList(
                                "setup_category_preview_lore", player, Map.entry("%id%", category.getId())))
                        .into(inventory, slot))
                .build();
    }

    private MenuButton skullButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.PLAYER_HEAD)
                        .name(messages.getString(
                                "setup_category_skull",
                                player,
                                Map.entry(
                                        "%texture%",
                                        category.getIconSkullTexture() == null
                                                ? messages.getString("setup_category_skull_none", player)
                                                : category.getIconSkullTexture())))
                        .lore(messages.getStringList("setup_category_skull_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) ->
                        new PlayerChatInput(plugin, player, "setup_category_skull_prompt", input -> {
                            String trimmed = input.strip();
                            if (trimmed.equalsIgnoreCase("none") || trimmed.equalsIgnoreCase("clear")) {
                                category.setIconSkullTexture(null);
                            } else if (trimmed.equalsIgnoreCase("viewer")) {
                                category.setIconSkullTexture(ItemBuilder.VIEWER_HEAD);
                            } else {
                                category.setIconSkullTexture(trimmed);
                            }
                            save(player);
                        }))
                .build();
    }

    private MenuButton visibilityButton(Visibility visibility, String nameKey) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    boolean active = category.getVisibilities().contains(visibility);
                    ItemBuilder.of(active ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE)
                            .name(messages.getString(nameKey, player))
                            .lore(messages.getStringList(
                                    active ? "setup_toggle_enabled" : "setup_toggle_disabled", player))
                            .glow(active)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    boolean active = category.getVisibilities().contains(visibility);
                    if (active && category.getVisibilities().size() == 1) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        messages.sendMessage(player, "setup_category_visibility_last");
                        return;
                    }
                    category.toggleVisibility(visibility);
                    save(player);
                })
                .build();
    }

    private MenuButton navigatorSlotButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.COMPARATOR)
                        .name(messages.getString(
                                "setup_category_slot",
                                player,
                                Map.entry("%slot%", String.valueOf(category.getNavigatorSlot()))))
                        .lore(messages.getStringList("setup_order_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    category.setNavigatorSlot(
                            Math.max(0, category.getNavigatorSlot() + (event.isRightClick() ? -1 : 1)));
                    save(player);
                })
                .build();
    }

    @Override
    protected void persist() {
        registry.persist(category);
    }

    @Override
    protected void reopen(Player player) {
        new CategoryEditorMenu(plugin, player, category).open(player);
    }

    @Override
    protected void openManagement(Player player) {
        new CategoryManagementMenu(plugin, player).open(player);
    }
}
