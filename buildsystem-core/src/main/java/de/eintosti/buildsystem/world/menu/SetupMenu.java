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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.world.menu.setup.CategoryManagementMenu;
import de.eintosti.buildsystem.world.menu.setup.DefaultIconsMenu;
import de.eintosti.buildsystem.world.menu.setup.NavigatorLayoutMenu;
import de.eintosti.buildsystem.world.menu.setup.StatusManagementMenu;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The {@code /setup} hub. Branches to the editors that customise BuildSystem's dynamic data: the default world-type
 * icons, the world {@code statuses}, and the navigator {@code categories}.
 */
@NullMarked
public class SetupMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_DEFAULT_ICONS = 10;
    private static final int SLOT_STATUSES = 12;
    private static final int SLOT_CATEGORIES = 14;
    private static final int SLOT_NAVIGATOR = 16;

    private final BuildSystemPlugin plugin;

    public SetupMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("setup_title", player));
        this.plugin = plugin;

        register(
                SLOT_DEFAULT_ICONS,
                hubButton(
                        XMaterial.GLOWSTONE_DUST,
                        "setup_default_icons_item",
                        p -> new DefaultIconsMenu(plugin, p).open(p)));
        register(
                SLOT_STATUSES,
                hubButton(XMaterial.NAME_TAG, "setup_statuses_item", p -> new StatusManagementMenu(plugin, p).open(p)));
        register(
                SLOT_CATEGORIES,
                hubButton(
                        XMaterial.FILLED_MAP,
                        "setup_categories_item",
                        p -> new CategoryManagementMenu(plugin, p).open(p)));
        register(
                SLOT_NAVIGATOR,
                hubButton(XMaterial.COMPASS, "setup_navigator_item", p -> new NavigatorLayoutMenu(plugin, p).open(p)));
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
        plugin.getMenuItems().fillAll(player, getInventory());
        renderButtons(player);
    }
}
