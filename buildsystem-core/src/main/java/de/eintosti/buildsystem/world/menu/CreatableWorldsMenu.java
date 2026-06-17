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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class CreatableWorldsMenu extends DisplayablesMenu {

    private static final Set<BuildWorldStatus> VALID_STATUSES = Sets.newHashSet(
            BuildWorldStatus.NOT_STARTED,
            BuildWorldStatus.IN_PROGRESS,
            BuildWorldStatus.ALMOST_FINISHED,
            BuildWorldStatus.FINISHED);

    static final String CREATE_WORLD_PROFILE = SkullTextures.ADD_ITEM;
    static final String CREATE_FOLDER_PROFILE = "69b861aabb316c4ed73b4e5428305782e735565ba2a053912e1efd834fa5a6f";

    private final PlayerServiceImpl playerService;

    protected CreatableWorldsMenu(
            BuildSystemPlugin plugin,
            Player player,
            NavigatorCategory category,
            String inventoryTitle,
            @Nullable String noWorldsMessage,
            Visibility requiredVisibility) {
        super(
                plugin,
                player,
                Options.builder()
                        .category(category)
                        .title(inventoryTitle)
                        .emptyMessage(noWorldsMessage)
                        .requiredVisibility(requiredVisibility)
                        .validStatuses(VALID_STATUSES)
                        .build());
        this.playerService = plugin.getPlayerService();
    }

    @Override
    protected void addExtraItems(Inventory inventory, Player player) {
        if (playerService.canCreateWorld(player, requiredVisibility)
                && player.hasPermission(getWorldCreationPermission())) {
            ItemBuilder.skull(Profileable.detect(CREATE_WORLD_PROFILE))
                    .name(plugin.getMessages().getString(getWorldCreationItemTitleKey(), player))
                    .into(inventory, 48);
        }
        if (player.hasPermission("buildsystem.create.folder")) {
            ItemBuilder.skull(Profileable.detect(CREATE_FOLDER_PROFILE))
                    .name(plugin.getMessages().getString("world_navigator_create_folder", player))
                    .into(inventory, 50);
        }
    }

    protected abstract String getWorldCreationPermission();

    protected abstract String getWorldCreationItemTitleKey();
}
