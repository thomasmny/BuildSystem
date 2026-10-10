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
package de.eintosti.buildsystem.menu;

import de.eintosti.buildsystem.i18n.Messages;
import org.jspecify.annotations.NullMarked;

/**
 * The collaborators every menu takes: messages, the shared menu items and the {@link Menus} factory for opening the
 * next menu. {@link Menus} builds one and passes it to each menu it opens.
 *
 * @param messages The plugin messages
 * @param menuItems The shared menu items
 * @param menus The factory for opening other menus
 */
@NullMarked
public record MenuContext(Messages messages, MenuItems menuItems, Menus menus) {}
