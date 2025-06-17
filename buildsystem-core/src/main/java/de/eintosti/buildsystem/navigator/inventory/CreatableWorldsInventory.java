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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.InventoryUtils;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract inventory class extending {@link DisplayablesInventory} that provides common functionality for inventories which allow the creation of new {@link BuildWorld}s and
 * {@link Folder}s.
 */
public abstract class CreatableWorldsInventory extends DisplayablesInventory {

    private static final Set<BuildWorldStatus> VALID_STATUSES = Sets.newHashSet(
            BuildWorldStatus.NOT_STARTED,
            BuildWorldStatus.IN_PROGRESS,
            BuildWorldStatus.ALMOST_FINISHED,
            BuildWorldStatus.FINISHED
    );

    private static final String CREATE_WORLD_PROFILE = "3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716";
    private static final String CREATE_FOLDER_PROFILE = "69b861aabb316c4ed73b4e5428305782e735565ba2a053912e1efd834fa5a6f";

    private final PlayerServiceImpl playerService;

    /**
     * Constructs a new {@link CreatableWorldsInventory}.
     *
     * @param plugin             The plugin instance
     * @param player             The player for whom this inventory is created
     * @param inventoryTitle     The inventory's title
     * @param noWorldsMessage    The "no worlds" message
     * @param requiredVisibility The required {@link Visibility} for worlds to be displayed in this inventory
     */
    protected CreatableWorldsInventory(
            @NotNull BuildSystemPlugin plugin,
            @NotNull Player player,
            @NotNull String inventoryTitle,
            @Nullable String noWorldsMessage,
            @NotNull Visibility requiredVisibility
    ) {
        super(plugin, player, inventoryTitle, noWorldsMessage, requiredVisibility, VALID_STATUSES);
        this.playerService = plugin.getPlayerService();
    }

    /**
     * Overrides the base method to include the "create world" and "create folder" items in the inventory page layout.
     *
     * @return A newly created {@link Inventory} page with common base items and creation options.
     */
    @Override
    protected @NotNull Inventory createBaseInventoryPage() {
        Inventory inventory = super.createBaseInventoryPage();
        addWorldCreateItem(inventory, player);
        addFolderCreateItem(inventory, player);
        return inventory;
    }

    private void addWorldCreateItem(Inventory inventory, Player player) {
        if (!playerService.canCreateWorld(player, requiredVisibility)) {
            return;
        }

        if (player.hasPermission(getWorldCreationPermission())) {
            inventory.setItem(48, InventoryUtils.createSkull(Messages.getString(getWorldCreationItemTitleKey(), player), Profileable.detect(CREATE_WORLD_PROFILE)));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 48);
        }
    }

    private void addFolderCreateItem(Inventory inventory, Player player) {
        if (player.hasPermission("buildsystem.create.folder")) {
            inventory.setItem(50, InventoryUtils.createSkull(Messages.getString("world_navigator_create_folder", player), Profileable.detect(CREATE_FOLDER_PROFILE)));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 50);
        }
    }

    /**
     * Gets the specific permission string required for a player to create a {@link BuildWorld} of the type managed by this inventory (e.g., {@code buildsystem.create.private},
     * {@code buildsystem.create.public}).
     *
     * @return The permission string for creating a world of this inventory's type.
     */
    protected abstract @NotNull String getWorldCreationPermission();

    /**
     * Gets the message key the title of the "create world" item.
     *
     * @return The message key for the "create world" item's title.
     */
    protected abstract @NotNull String getWorldCreationItemTitleKey();
}