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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.util.InventoryUtils;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ArchivedWorldsInventory extends FilteredWorldsInventory {

    private static final Visibility VISIBILITY = Visibility.IGNORE;
    private static final Set<BuildWorldStatus> VALID_STATUS = Sets.newHashSet(BuildWorldStatus.ARCHIVE);

    public ArchivedWorldsInventory(BuildSystemPlugin plugin) {
        super(plugin, "archive_title", "archive_no_worlds", VISIBILITY, VALID_STATUS);
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = super.createInventory(player);
        InventoryUtils.addGlassPane(player, inventory, 49);
        return inventory;
    }
}