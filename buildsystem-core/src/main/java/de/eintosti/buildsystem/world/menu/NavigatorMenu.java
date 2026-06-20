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
package de.eintosti.buildsystem.world.menu;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.CategoryPermissions;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The inventory ("old") navigator. Its layout is derived from the {@link NavigatorCategory} registry: every category
 * shown in the navigator is placed at its configured slot, so adding or restyling a category is reflected here without
 * code changes. The settings button keeps a fixed slot.
 */
@NullMarked
public class NavigatorMenu extends ButtonMenu<MenuButton> {

    private static final int INVENTORY_SIZE = 27;

    private final MenuItems menuItems;
    private final Menus menus;

    public NavigatorMenu(
            Messages messages,
            MenuItems menuItems,
            Menus menus,
            NavigatorCategoryRegistryImpl navigatorCategoryRegistry,
            Player player) {
        super(messages, INVENTORY_SIZE, messages.getString("old_navigator_title", player));
        this.menuItems = menuItems;
        this.menus = menus;

        int settingsSlot = navigatorCategoryRegistry.getSettingsSlot();
        for (NavigatorCategory category : navigatorCategoryRegistry.getCategories()) {
            int slot = category.getNavigatorSlot();
            if (!category.isShownInNavigator() || slot < 0 || slot >= INVENTORY_SIZE || slot == settingsSlot) {
                continue;
            }
            if (!CategoryPermissions.canAccess(player, category.getId())) {
                continue;
            }
            register(slot, categoryButton(category));
        }

        if (settingsSlot >= 0 && settingsSlot < INVENTORY_SIZE) {
            register(settingsSlot, settingsButton());
        }
    }

    private MenuButton categoryButton(NavigatorCategory category) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> menuItems.renderCategoryIcon(
                        inventory, slot, category, player, ColorAPI.process(category.getStyledName()), List.of()))
                .onClick((player, event) -> {
                    menus.openCategoryWorlds(category, player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                })
                .build();
    }

    private MenuButton settingsButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                        .name(messages.getString("old_navigator_settings", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    if (!player.hasPermission("buildsystem.settings")) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        return;
                    }
                    menus.openSettings(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        menuItems.fillRange(player, getInventory(), 0, INVENTORY_SIZE);
        renderButtons(player);
    }
}
