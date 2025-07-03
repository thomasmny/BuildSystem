/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.world.navigator.inventory;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * An inventory class specifically for displaying and managing private {@link BuildWorld}s.
 */
@NullMarked
public class PrivateWorldsInventory extends CreatableWorldsInventory {

    /**
     * Constructs a new {@link PrivateWorldsInventory} instance.
     *
     * @param plugin The plugin instance
     * @param player The player for whom this inventory is created
     */
    public PrivateWorldsInventory(BuildSystemPlugin plugin, Player player) {
        super(
                plugin,
                player,
                NavigatorCategory.PRIVATE,
                Messages.getString("private_title", player),
                Messages.getString("private_no_worlds", player),
                Visibility.PRIVATE
        );
    }

    /**
     * Returns the permission string required to create a private world.
     *
     * @return The permission string: "buildsystem.create.private".
     */
    @Override
    protected String getWorldCreationPermission() {
        return "buildsystem.create.private";
    }

    /**
     * Returns the message key for the title of the "create private world" item.
     *
     * @return The message key: "private_create_world".
     */
    @Override
    protected String getWorldCreationItemTitleKey() {
        return "private_create_world";
    }
}