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

import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.util.FeedbackSound;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The {@code /setup} hub. Branches to the editors that customise BuildSystem's dynamic data: the default world-type
 * icons, the world {@code statuses}, and the navigator (categories and their layout, managed together).
 */
@NullMarked
public class SetupMenu extends ButtonMenu {

    private static final int SLOT_DEFAULT_ICONS = 11;
    private static final int SLOT_STATUSES = 13;
    private static final int SLOT_NAVIGATOR = 15;

    public SetupMenu(MenuContext context, Player player) {
        super(context, 27, context.messages().getString("setup_title", player));

        register(
                SLOT_DEFAULT_ICONS,
                hubButton(Material.ITEM_FRAME, "setup_default_icons_item", menus::openDefaultIcons));
        register(SLOT_STATUSES, hubButton(Material.NAME_TAG, "setup_statuses_item", menus::openStatusLayout));
        register(SLOT_NAVIGATOR, hubButton(Material.COMPASS, "setup_navigator_item", menus::openNavigatorLayout));
    }

    private MenuButton hubButton(Material icon, String nameKey, Consumer<Player> open) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(icon)
                        .name(messages.getString(nameKey, player))
                        .lore(messages.getStringList(nameKey + "_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    FeedbackSound.OPEN.play(player);
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
