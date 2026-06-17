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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.ArrayList;
import java.util.List;
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

        List<MenuButton> properties = new ArrayList<>();
        properties.add(
                renameButton("setup_category_rename", "setup_category_rename_prompt", this.category::setDisplayName));
        properties.add(colorButton("setup_category_color", this.category::getColor, this.category::setColor));
        properties.add(iconButton());
        properties.add(
                visibilityButton(Visibility.EVERYONE, XMaterial.ENDER_EYE, "setup_category_visibility_everyone"));
        properties.add(
                visibilityButton(Visibility.ADDED_PLAYERS, XMaterial.ENDER_PEARL, "setup_category_visibility_added"));
        properties.add(statusesButton());
        registerCentered(properties);
        register(SLOT_BACK, backButton());
    }

    /**
     * The category icon button. Left-click opens the item picker to choose the material. When that material is a player
     * head, the skull texture is part of the same control: right-click prompts for it (a texture, {@code viewer} for the
     * viewing player's head, or {@code none} to clear).
     */
    private MenuButton iconButton() {
        boolean isHead = category.getIcon() == XMaterial.PLAYER_HEAD;
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.icon(category, player)
                        .name(messages.getString("setup_category_icon", player))
                        .lore(messages.getStringList(
                                isHead ? "setup_category_icon_head_lore" : "setup_category_icon_lore",
                                player,
                                Map.entry("%texture%", skullState(player))))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    if (isHead && event.isRightClick()) {
                        promptSkullTexture(player);
                        return;
                    }
                    new MaterialPickerMenu(
                                    plugin,
                                    player,
                                    material -> {
                                        category.setIcon(material);
                                        save(player);
                                    },
                                    () -> reopen(player))
                            .open(player);
                })
                .build();
    }

    private void promptSkullTexture(Player player) {
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
        });
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

    private MenuButton visibilityButton(Visibility visibility, XMaterial icon, String nameKey) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    boolean active = category.getVisibilities().contains(visibility);
                    String state = messages.getString(
                            active ? "setup_category_visibility_state_on" : "setup_category_visibility_state_off",
                            player);
                    ItemBuilder.of(icon)
                            .name(messages.getString(nameKey, player))
                            .lore(messages.getStringList(nameKey + "_lore", player, Map.entry("%state%", state)))
                            .glow(active)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    category.toggleVisibility(visibility);
                    save(player);
                })
                .build();
    }

    private MenuButton statusesButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    List<String> lore = new ArrayList<>(messages.getStringList("setup_category_statuses_lore", player));
                    lore.add(messages.getString("setup_category_statuses_members", player));
                    if (category.getStatusIds().isEmpty()) {
                        lore.add(messages.getString("setup_category_statuses_none", player));
                    } else {
                        for (String statusId : category.getStatusIds()) {
                            String name = plugin.getWorldStatusRegistry()
                                    .getStatus(statusId)
                                    .map(status -> ColorAPI.process(status.getStyledName()))
                                    .orElse(statusId);
                            lore.add(messages.getString(
                                    "setup_category_statuses_member_entry", player, Map.entry("%status%", name)));
                        }
                    }
                    lore.add("");
                    lore.add(messages.getString("setup_category_statuses_hint", player));
                    ItemBuilder.of(XMaterial.BOOK)
                            .name(messages.getString("setup_category_statuses", player))
                            .lore(lore)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> new CategoryStatusesMenu(plugin, player, category).open(player))
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
