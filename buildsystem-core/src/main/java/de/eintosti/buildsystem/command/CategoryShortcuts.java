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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.CategorySubCommand;
import de.eintosti.buildsystem.world.display.CategoryPermissions;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Turns the live navigator categories into {@code /worlds <category>} shortcut subcommands, so adding a category makes
 * its shortcut available with no code change and deleting one removes it. Each shortcut opens that category in the
 * navigator; access is governed by {@link CategoryPermissions}.
 */
@NullMarked
public final class CategoryShortcuts implements DynamicSubCommands {

    private final Services services;

    public CategoryShortcuts(Services services) {
        this.services = services;
    }

    @Override
    public Optional<SubCommand> resolve(String name) {
        return services.navigatorCategoryRegistry()
                .getCategory(name.toLowerCase(Locale.ROOT))
                .map(category -> categoryCommand(category.getId()));
    }

    @Override
    public List<SubCommand> available(Player player) {
        return services.navigatorCategoryRegistry().getCategories().stream()
                .filter(category -> CategoryPermissions.canAccess(player, category.getId()))
                .<SubCommand>map(category -> categoryCommand(category.getId()))
                .toList();
    }

    private CategorySubCommand categoryCommand(String categoryId) {
        return new CategorySubCommand(
                services.messages(),
                services.world(),
                services.navigatorCategoryRegistry(),
                services.menus(),
                categoryId);
    }
}
