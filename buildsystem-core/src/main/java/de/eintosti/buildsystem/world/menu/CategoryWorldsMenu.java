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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The single navigator listing for any {@link NavigatorCategory}, both the built-in categories ({@code public},
 * {@code private}, {@code archive}) and any admin-defined one. It lists the category's {@link BuildWorld}s and folders
 * and offers world/folder creation.
 *
 * <p>World creation is offered dynamically: a category shows the "create world" item only when a freshly created world
 * — which always starts at the registry's {@link de.eintosti.buildsystem.api.world.data.WorldStatusRegistry#getDefault()
 * default status} — would actually be grouped into this category, and the player holds the per-category create
 * permission {@code buildsystem.create.category.<categoryId>} (e.g. {@code buildsystem.create.category.public}). This
 * is why the archive
 * category, which never contains the default status, never offers world creation.
 */
@NullMarked
public class CategoryWorldsMenu extends DisplayablesMenu {

    private final WorldStatusRegistryImpl worldStatusRegistry;

    public CategoryWorldsMenu(
            DisplayablesContext context,
            WorldStatusRegistryImpl worldStatusRegistry,
            Player player,
            NavigatorCategory category) {
        super(
                context,
                player,
                Options.builder()
                        .category(category)
                        .title(context.menuContext()
                                .messages()
                                .getString(
                                        "category_title",
                                        player,
                                        Placeholders.of("%category%", category.getDisplayName())))
                        .emptyMessage(context.menuContext().messages().getString("world_navigator_no_worlds", player))
                        .build());
        this.worldStatusRegistry = worldStatusRegistry;
    }

    @Override
    protected CreateButtons createButtons(Player player) {
        return new CreateButtons(canCreateWorldHere(player), player.hasPermission(Permissions.CREATE_FOLDER));
    }

    /**
     * A category offers world creation only when a freshly created world (which starts at the default status) would be
     * grouped here, the player is allowed to create another world of this visibility, and the player holds the
     * per-category create permission.
     */
    private boolean canCreateWorldHere(Player player) {
        String defaultStatusId = worldStatusRegistry.getDefault().getId();
        return category.getStatusIds().contains(defaultStatusId)
                && playerService.canCreateWorld(player, category.getPrimaryVisibility())
                && hasCreatePermission(player);
    }

    /**
     * Admins may create worlds in any category; everyone else needs the per-category create node
     * {@code buildsystem.create.category.<categoryId>}. This mirrors how the admin permission grants an unlimited
     * world count.
     */
    private boolean hasCreatePermission(Player player) {
        return player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)
                || player.hasPermission(Permissions.createInCategory(category.getId()));
    }
}
