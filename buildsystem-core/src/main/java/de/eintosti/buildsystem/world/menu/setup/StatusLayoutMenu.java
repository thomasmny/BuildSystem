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

import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.display.Registry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * The status setup: the preview is a live {@code /worlds setStatus} picker, and statuses are dragged into it from the
 * palette. All of the drag-and-drop lives in {@link LayoutEditorMenu}; this class supplies only the registry, the
 * icon, the message keys, and where shift-click goes.
 */
@NullMarked
public class StatusLayoutMenu extends LayoutEditorMenu<BuildWorldStatus> {

    private static final LayoutMessages MESSAGES = new LayoutMessages(
            "setup_status_add",
            "setup_status_layout_create_lore",
            "setup_status_layout_reset",
            "setup_status_layout_reset_lore",
            "setup_status_layout_reset_confirm_lore",
            "setup_status_layout_reset_all_confirm_lore",
            "setup_status_layout_delete",
            "setup_status_layout_delete_lore",
            "setup_status_layout_placed_lore",
            "setup_status_layout_palette_lore",
            "setup_status_add_prompt");

    private final WorldStatusRegistryImpl registry;

    public StatusLayoutMenu(
            Messages messages,
            MenuItems menuItems,
            Menus menus,
            TaskScheduler scheduler,
            Prompts prompts,
            WorldStatusRegistryImpl worldStatusRegistry,
            NavigatorEditorService navigatorEditorService,
            Player player) {
        super(
                messages,
                WorldStatusRegistryImpl.STATUS_MENU_SIZE,
                messages.getString("setup_statuses_title", player),
                MESSAGES,
                menuItems,
                menus,
                scheduler,
                prompts,
                navigatorEditorService);
        this.registry = worldStatusRegistry;
    }

    @Override
    protected Registry<BuildWorldStatus> registry() {
        return registry;
    }

    @Override
    protected void renderIcon(
            BuildWorldStatus status, Player player, Inventory inventory, int slot, String name, List<String> lore) {
        ItemBuilder.of(status.getIcon()).name(name).lore(lore).into(inventory, slot);
    }

    @Override
    protected void openEditor(BuildWorldStatus status, Player player) {
        menus.openStatusEditor(status, player);
    }

    @Override
    protected void reopen(Player player) {
        menus.openStatusLayout(player);
    }
}
