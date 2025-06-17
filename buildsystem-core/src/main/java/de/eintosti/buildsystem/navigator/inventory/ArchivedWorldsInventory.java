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
package de.eintosti.buildsystem.navigator.inventory;

import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class ArchivedWorldsInventory extends DisplayablesInventory {

    public ArchivedWorldsInventory(BuildSystemPlugin plugin, Player player) {
        super(
                plugin,
                player,
                Messages.getString("archive_title", player),
                Messages.getString("archive_no_worlds", player),
                Visibility.IGNORE,
                Sets.newHashSet(BuildWorldStatus.ARCHIVE)
        );
    }

    @Override
    protected @NotNull Inventory createBaseInventoryPage() {
        Inventory inventory = super.createBaseInventoryPage();
        InventoryUtils.addGlassPane(player, inventory, 49);
        return inventory;
    }
}