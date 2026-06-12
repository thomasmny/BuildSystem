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
package de.eintosti.buildsystem.world.navigator.inventory;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class CreatableWorldsInventory extends DisplayablesInventory {

    private static final Set<BuildWorldStatus> VALID_STATUSES = Sets.newHashSet(
            BuildWorldStatus.NOT_STARTED,
            BuildWorldStatus.IN_PROGRESS,
            BuildWorldStatus.ALMOST_FINISHED,
            BuildWorldStatus.FINISHED
    );

    static final String CREATE_WORLD_PROFILE = "3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716";
    static final String CREATE_FOLDER_PROFILE = "69b861aabb316c4ed73b4e5428305782e735565ba2a053912e1efd834fa5a6f";

    private final PlayerServiceImpl playerService;

    protected CreatableWorldsInventory(
            BuildSystemPlugin plugin,
            Player player,
            NavigatorCategory category,
            String inventoryTitle,
            @Nullable String noWorldsMessage,
            Visibility requiredVisibility
    ) {
        super(plugin, player, category, inventoryTitle, noWorldsMessage, requiredVisibility, VALID_STATUSES);
        this.playerService = plugin.getPlayerService();
    }

    @Override
    protected void addExtraItems(Inventory inventory, Player player) {
        if (playerService.canCreateWorld(player, requiredVisibility) && player.hasPermission(getWorldCreationPermission())) {
            inventory.setItem(48, InventoryUtils.createSkull(plugin.getMessages().getString(getWorldCreationItemTitleKey(), player), Profileable.detect(CREATE_WORLD_PROFILE)));
        }
        if (player.hasPermission("buildsystem.create.folder")) {
            inventory.setItem(50, InventoryUtils.createSkull(plugin.getMessages().getString("world_navigator_create_folder", player), Profileable.detect(CREATE_FOLDER_PROFILE)));
        }
    }

    protected abstract String getWorldCreationPermission();

    protected abstract String getWorldCreationItemTitleKey();
}
