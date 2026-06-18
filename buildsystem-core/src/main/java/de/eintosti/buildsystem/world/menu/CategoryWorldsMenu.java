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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * The single navigator listing for any {@link NavigatorCategory}, both the built-in categories ({@code public},
 * {@code private}, {@code archive}) and any admin-defined one. It lists the category's {@link BuildWorld}s and folders
 * and offers world/folder creation.
 *
 * <p>World creation is offered dynamically: a category shows the "create world" item only when a freshly created world
 * — which always starts at the registry's {@link de.eintosti.buildsystem.api.world.data.WorldStatusRegistry#getDefaultStatus()
 * default status} — would actually be grouped into this category, and the player holds the per-category create
 * permission {@code buildsystem.create.<categoryId>} (e.g. {@code buildsystem.create.public}). This is why the archive
 * category, which never contains the default status, never offers world creation.
 */
@NullMarked
public class CategoryWorldsMenu extends DisplayablesMenu {

    static final String CREATE_WORLD_PROFILE = SkullTextures.ADD_ITEM;
    static final String CREATE_FOLDER_PROFILE = "69b861aabb316c4ed73b4e5428305782e735565ba2a053912e1efd834fa5a6f";

    private static final int SLOT_CREATE_WORLD = 48;
    private static final int SLOT_CREATE_FOLDER = 50;
    private static final int SLOT_CREATE_CENTER = 49;

    public CategoryWorldsMenu(BuildSystemPlugin plugin, Player player, NavigatorCategory category) {
        super(
                plugin,
                player,
                Options.builder()
                        .category(category)
                        .title(ColorAPI.process(category.getDisplayName()))
                        .emptyMessage(plugin.getMessages().getString("world_navigator_no_worlds", player))
                        .build());
    }

    @Override
    protected void addExtraItems(Inventory inventory, Player player) {
        boolean createWorld = canCreateWorldHere(player);
        if (createWorld) {
            ItemBuilder.skull(Profileable.detect(CREATE_WORLD_PROFILE))
                    .name(plugin.getMessages().getString("world_navigator_create_world", player))
                    .into(inventory, SLOT_CREATE_WORLD);
        }
        if (player.hasPermission("buildsystem.create.folder")) {
            // With the create-world button hidden, centre the lone folder button instead of leaving it off to the side.
            int folderSlot = createWorld ? SLOT_CREATE_FOLDER : SLOT_CREATE_CENTER;
            ItemBuilder.skull(Profileable.detect(CREATE_FOLDER_PROFILE))
                    .name(plugin.getMessages().getString("world_navigator_create_folder", player))
                    .into(inventory, folderSlot);
        }
    }

    /**
     * A category offers world creation only when a freshly created world (which starts at the default status) would be
     * grouped here, the player is allowed to create another world of this visibility, and the player holds the
     * per-category create permission.
     */
    private boolean canCreateWorldHere(Player player) {
        String defaultStatusId =
                plugin.getWorldStatusRegistry().getDefaultStatus().getId();
        return category.getStatusIds().contains(defaultStatusId)
                && playerService.canCreateWorld(player, category.getPrimaryVisibility())
                && hasCreatePermission(player);
    }

    /**
     * Admins may create worlds in any category; everyone else needs the per-category create node
     * {@code buildsystem.create.<categoryId>}. This mirrors how the admin permission grants an unlimited world count.
     */
    private boolean hasCreatePermission(Player player) {
        return WorldPermissionsImpl.of(plugin, null).hasAdminPermission(player)
                || player.hasPermission("buildsystem.create." + category.getId());
    }
}
