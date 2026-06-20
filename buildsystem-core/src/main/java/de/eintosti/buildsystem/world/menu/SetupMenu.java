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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The {@code /setup} hub. Branches to the editors that customise BuildSystem's dynamic data: the default world-type
 * icons, the world {@code statuses}, and the navigator (categories and their layout, managed together).
 */
@NullMarked
public class SetupMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_DEFAULT_ICONS = 11;
    private static final int SLOT_STATUSES = 13;
    private static final int SLOT_NAVIGATOR = 15;

    private final MenuItems menuItems;
    private final Menus menus;

    public SetupMenu(Messages messages, MenuItems menuItems, Menus menus, Player player) {
        super(messages, 27, messages.getString("setup_title", player));
        this.menuItems = menuItems;
        this.menus = menus;

        register(
                SLOT_DEFAULT_ICONS,
                hubButton(XMaterial.ITEM_FRAME, "setup_default_icons_item", menus::openDefaultIcons));
        register(SLOT_STATUSES, hubButton(XMaterial.NAME_TAG, "setup_statuses_item", menus::openStatusLayout));
        register(SLOT_NAVIGATOR, hubButton(XMaterial.COMPASS, "setup_navigator_item", menus::openNavigatorLayout));
    }

    private MenuButton hubButton(XMaterial icon, String nameKey, Consumer<Player> open) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(icon)
                        .name(messages.getString(nameKey, player))
                        .lore(messages.getStringList(nameKey + "_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    open.accept(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        menuItems.fillAll(player, getInventory());
        renderButtons(player);
    }
}
