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
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.world.display.CategoryPermissions;
import de.eintosti.buildsystem.world.menu.CategoryWorldsMenu;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * A {@code /worlds <category>} shortcut that opens a single navigator category, replacing the old hardcoded
 * {@code archive}/{@code private}/{@code public} subcommands. One is created per registered category by
 * {@link de.eintosti.buildsystem.command.CategoryShortcuts}; access is governed by {@link CategoryPermissions}, the same
 * permission that decides whether the category is shown in the navigator.
 */
@NullMarked
public class CategorySubCommand extends AbstractSubCommand {

    private final String categoryId;
    private final Argument argument;

    public CategorySubCommand(BuildSystemPlugin plugin, String categoryId) {
        super(plugin);
        this.categoryId = categoryId;
        this.argument = new CategoryArgument(categoryId);
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        NavigatorCategoryRegistry registry = plugin.getNavigatorCategoryRegistry();
        NavigatorCategory category = registry.getCategory(categoryId).orElse(null);
        if (category == null) {
            // The category was deleted between the shortcut being listed and this invocation.
            messages.sendMessage(player, "worlds_unknown_command");
            return;
        }
        new CategoryWorldsMenu(plugin, player, category).open(player);
    }

    @Override
    public boolean hasPermission(Player player) {
        return CategoryPermissions.canAccess(player, categoryId);
    }

    @Override
    public Argument getArgument() {
        return argument;
    }

    /**
     * The {@link Argument} for a category shortcut: its name is the category id and its permission is the shared
     * per-category node, so help and tab-completion describe it consistently.
     */
    private record CategoryArgument(String categoryId) implements Argument {

        @Override
        public String getName() {
            return categoryId;
        }

        @Override
        public String getPermission() {
            return CategoryPermissions.node(categoryId);
        }
    }
}
