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

import static de.eintosti.buildsystem.world.menu.CreatableWorldsMenu.CREATE_FOLDER_PROFILE;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ArchivedWorldsMenu extends DisplayablesMenu {

    public ArchivedWorldsMenu(BuildSystemPlugin plugin, Player player) {
        super(
                plugin,
                player,
                NavigatorCategory.ARCHIVE,
                plugin.getMessages().getString("archive_title", player),
                plugin.getMessages().getString("archive_no_worlds", player),
                Visibility.IGNORE,
                Sets.newHashSet(BuildWorldStatus.ARCHIVE));
    }

    @Override
    protected void addExtraItems(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.create.folder")) {
            inventory.setItem(
                    49,
                    InventoryUtils.createSkull(
                            plugin.getMessages().getString("world_navigator_create_folder", player),
                            Profileable.detect(CREATE_FOLDER_PROFILE)));
        }
    }
}
