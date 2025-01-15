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
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.data.WorldStatus;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PrivateInventory extends FilteredWorldsInventory {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;
    private final PlayerManager playerManager;

    public PrivateInventory(BuildSystem plugin) {
        super(plugin, "private_title", "private_no_worlds", Visibility.PRIVATE,
                Sets.newHashSet(WorldStatus.NOT_STARTED, WorldStatus.IN_PROGRESS, WorldStatus.ALMOST_FINISHED, WorldStatus.FINISHED)
        );

        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        this.playerManager = plugin.getPlayerManager();
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = super.createInventory(player);
        if (playerManager.canCreateWorld(player, super.getVisibility())) {
            addWorldCreateItem(inventory, player);
        }
        return inventory;
    }

    private void addWorldCreateItem(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.create.private")) {
            inventoryUtils.addSkull(inventory, 49, Messages.getString("private_create_world", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716"));
        } else {
            inventoryUtils.addGlassPane(plugin, player, inventory, 49);
        }
    }
}