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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Toggles which {@link BuildWorldStatus statuses} a {@link de.eintosti.buildsystem.api.world.display.NavigatorCategory
 * category} groups. A glowing entry is a member; clicking adds or removes it. Statuses are shared, so the same status
 * may belong to several categories.
 */
@NullMarked
public class CategoryStatusesMenu extends PaginatedMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int FIRST_CONTENT_SLOT = 9;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_PREVIOUS_PAGE = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl registry;
    private final NavigatorCategoryImpl category;

    public CategoryStatusesMenu(BuildSystemPlugin plugin, Player player, NavigatorCategoryImpl category) {
        super(plugin.getMessages(), 54, plugin.getMessages().getString("setup_category_statuses_title", player));
        this.plugin = plugin;
        this.registry = plugin.getNavigatorCategoryRegistry();
        this.category = category;
    }

    @Override
    protected int totalItems() {
        return plugin.getWorldStatusRegistry().getStatuses().size();
    }

    @Override
    protected void populate(Player player) {
        clearButtons();
        // Frame the whole menu so the (usually mostly empty) grid reads as a deliberate panel, not a void.
        plugin.getMenuItems().fillAll(player, getInventory());

        List<BuildWorldStatus> statuses =
                List.copyOf(plugin.getWorldStatusRegistry().getStatuses());
        registerPageItems(FIRST_CONTENT_SLOT, ITEMS_PER_PAGE, statuses, this::statusToggle);

        register(SLOT_BACK, backButton());
        // Page arrows only when the statuses actually overflow a single page.
        if (totalPages(ITEMS_PER_PAGE) > 1) {
            register(SLOT_PREVIOUS_PAGE, previousPageButton(SkullTextures.PREVIOUS_PAGE, ITEMS_PER_PAGE));
            register(SLOT_NEXT_PAGE, nextPageButton(SkullTextures.NEXT_PAGE, ITEMS_PER_PAGE));
        }

        renderButtons(player);
    }

    private MenuButton statusToggle(BuildWorldStatus status) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    boolean member = category.getStatusIds().contains(status.getId());
                    // Always show the status's own icon; membership is conveyed by the glow and the lore.
                    ItemBuilder.of(status.getIcon())
                            .name(ColorAPI.process(status.getStyledName()))
                            .lore(messages.getStringList(
                                    member ? "setup_category_status_member" : "setup_category_status_not_member",
                                    player))
                            .glow(member)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    if (category.getStatusIds().contains(status.getId())) {
                        category.removeStatusId(status.getId());
                    } else {
                        category.addStatusId(status.getId());
                    }
                    registry.persist(category);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    populate(player);
                })
                .build();
    }

    private MenuButton backButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.BARRIER)
                        .name(messages.getString("setup_back", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    new CategoryEditorMenu(plugin, player, category).open(player);
                })
                .build();
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Filler clicks do nothing; navigation is via the explicit buttons.
    }
}
