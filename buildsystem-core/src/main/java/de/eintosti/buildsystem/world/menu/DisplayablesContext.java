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

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import org.jspecify.annotations.NullMarked;

/**
 * The shared collaborators every {@link DisplayablesMenu} needs. Bundling them keeps the base constructor — and the
 * subclasses that forward to it — to a handful of parameters instead of a long, transposable row of services. The
 * {@link Menus} factory assembles one and hands it to each menu it opens.
 *
 * @param messages The message service
 * @param playerService The player service
 * @param settingsService The settings service
 * @param worldService The world service, queried for its folder and world storages
 * @param menuItems The shared menu-item renderer
 * @param prompts The chat-input prompt factory
 * @param navigatorService The navigator service
 * @param menus The menu factory/navigation hub
 */
@NullMarked
public record DisplayablesContext(
        Messages messages,
        PlayerServiceImpl playerService,
        SettingsService settingsService,
        WorldServiceImpl worldService,
        MenuItems menuItems,
        Prompts prompts,
        NavigatorService navigatorService,
        Menus menus) {}
