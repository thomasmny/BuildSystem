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
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
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

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 36;
    private static final int FIRST_CONTENT_SLOT = 9;

    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS_PAGE = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl registry;
    private final NavigatorCategoryImpl category;

    public CategoryStatusesMenu(BuildSystemPlugin plugin, Player player, NavigatorCategory category) {
        super(
                plugin.getMessages(),
                INVENTORY_SIZE,
                plugin.getMessages().getString("setup_category_statuses_title", player));
        this.plugin = plugin;
        this.registry = plugin.getNavigatorCategoryRegistry();
        this.category = (NavigatorCategoryImpl) category;
    }

    @Override
    protected int totalItems() {
        return plugin.getWorldStatusRegistry().getStatuses().size();
    }

    @Override
    protected void populate(Player player) {
        clearButtons();

        // Top + bottom glass border with a hollow middle, matching the status/category management menus.
        plugin.getMenuItems().fillWithGlass(getInventory(), player);

        List<BuildWorldStatus> statuses =
                List.copyOf(plugin.getWorldStatusRegistry().getStatuses());
        registerPageItems(FIRST_CONTENT_SLOT, ITEMS_PER_PAGE, statuses, this::createStatusToggle);

        register(SLOT_BACK, createBackButton());
        setupPaginationArrows();

        renderButtons(player);
    }

    private void setupPaginationArrows() {
        if (totalPages(ITEMS_PER_PAGE) <= 1) {
            return;
        }
        register(SLOT_PREVIOUS_PAGE, previousPageButton(SkullTextures.PREVIOUS_PAGE, ITEMS_PER_PAGE));
        register(SLOT_NEXT_PAGE, nextPageButton(SkullTextures.NEXT_PAGE, ITEMS_PER_PAGE));
    }

    private MenuButton createStatusToggle(BuildWorldStatus status) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    boolean isMember = isCategoryMember(status);
                    String loreKey = isMember ? "setup_category_status_member" : "setup_category_status_not_member";

                    // Always show the status's own icon; membership is conveyed by the glow and the lore.
                    ItemBuilder.of(status.getIcon())
                            .name(ColorAPI.process(status.getStyledName()))
                            .lore(messages.getStringList(loreKey, player))
                            .glow(isMember)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    toggleMembership(status);
                    registry.persist(category);

                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    populate(player);
                })
                .build();
    }

    private MenuButton createBackButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.BARRIER)
                        .name(messages.getString("setup_back", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    plugin.getMenus().openCategoryEditor(category, player);
                })
                .build();
    }

    private boolean isCategoryMember(BuildWorldStatus status) {
        return category.getStatusIds().contains(status.getId());
    }

    private void toggleMembership(BuildWorldStatus status) {
        if (isCategoryMember(status)) {
            category.removeStatusId(status.getId());
        } else {
            category.addStatusId(status.getId());
        }
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Filler clicks do nothing; navigation is via the explicit buttons.
    }
}
