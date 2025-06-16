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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.InventoryUtils;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PublicWorldsInventory extends FilteredWorldsInventory {

    private static final Visibility VISIBILITY = Visibility.PUBLIC;
    private static final Set<BuildWorldStatus> VALID_STATUS = Sets.newHashSet(
            BuildWorldStatus.NOT_STARTED, BuildWorldStatus.IN_PROGRESS, BuildWorldStatus.ALMOST_FINISHED, BuildWorldStatus.FINISHED
    );

    private final PlayerServiceImpl playerManager;

    public PublicWorldsInventory(BuildSystemPlugin plugin) {
        super(plugin, "world_navigator_title", "world_navigator_no_worlds", VISIBILITY, VALID_STATUS);
        this.playerManager = plugin.getPlayerService();
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = super.createInventory(player);
        if (playerManager.canCreateWorld(player, super.getVisibility())) {
            addWorldCreateItem(inventory, player);
        }
        addFolderCreateItem(inventory, player);
        return inventory;
    }

    private void addWorldCreateItem(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.create.public")) {
            inventory.setItem(49, InventoryUtils.createSkull(Messages.getString("world_navigator_create_world", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716")));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 49);
        }
    }

    private void addFolderCreateItem(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.create.folder")) {
            inventory.setItem(50, InventoryUtils.createSkull(Messages.getString("world_navigator_create_folder", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716")));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 50);
        }
    }
}