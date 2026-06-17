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

    private static final int SLOT_RENAME = 10;
    private static final int SLOT_COLOR = 11;
    private static final int SLOT_ICON = 12;
    private static final int SLOT_EVERYONE = 13;
    private static final int SLOT_ADDED_PLAYERS = 14;
    private static final int SLOT_STATUSES = 15;
    private static final int SLOT_SKULL = 16;
    private static final int SLOT_BACK = 18;

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

        register(
                SLOT_RENAME,
                renameButton("setup_category_rename", "setup_category_rename_prompt", this.category::setDisplayName));
        register(SLOT_COLOR, colorButton("setup_category_color", this.category::setColor));
        register(SLOT_ICON, iconButton("setup_category_icon", this.category::getIcon, this.category::setIcon));
        register(SLOT_EVERYONE, visibilityButton(Visibility.EVERYONE, "setup_category_visibility_everyone"));
        register(SLOT_ADDED_PLAYERS, visibilityButton(Visibility.ADDED_PLAYERS, "setup_category_visibility_added"));
        register(
                SLOT_STATUSES,
                labelled(
                        XMaterial.NAME_TAG,
                        "setup_category_statuses",
                        (p, event) -> new CategoryStatusesMenu(plugin, p, this.category).open(p)));
        // The skull-texture option only makes sense for a player-head icon, so it appears only then (keeping the
        // property row contiguous as the last entry otherwise).
        if (this.category.getIcon() == XMaterial.PLAYER_HEAD) {
            register(SLOT_SKULL, skullButton());
        }
        register(SLOT_BACK, backButton());
    }

    private MenuButton skullButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.PLAYER_HEAD)
                        .name(messages.getString(
                                "setup_category_skull", player, Map.entry("%state%", skullState(player))))
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

    /**
     * {@return a short label for the current skull texture} Avoids rendering the full texture hash in the item name:
     * {@code none}, the viewer-head label, or a generic "custom" label.
     */
    private String skullState(Player player) {
        String texture = category.getIconSkullTexture();
        if (texture == null || texture.isBlank()) {
            return messages.getString("setup_category_skull_none", player);
        }
        if (ItemBuilder.VIEWER_HEAD.equals(texture)) {
            return messages.getString("setup_category_skull_viewer", player);
        }
        return messages.getString("setup_category_skull_custom", player);
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
        new NavigatorLayoutMenu(plugin, player).open(player);
    }
}
