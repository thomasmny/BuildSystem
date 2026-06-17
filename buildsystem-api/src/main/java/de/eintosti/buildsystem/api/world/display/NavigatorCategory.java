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
package de.eintosti.buildsystem.api.world.display;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A user-defined navigator category that groups worlds by a combination of {@link Visibility world visibility} and
 * the {@link BuildWorldStatus statuses} it {@link #getStatusIds() contains}. The category a world is displayed in is
 * resolved through {@link NavigatorCategoryRegistry#getCategoryForWorld(BuildWorld)}: a world belongs to a category
 * when the category's visibilities contain the world's and the category contains the world's status. Statuses are
 * shared and may appear in several categories (e.g. {@code in_progress} lives in both the public and private
 * categories, distinguished by visibility).
 *
 * <p>Server administrators can create, restyle, and delete categories at runtime through the in-game setup menu.
 * Built-in categories ({@code public}, {@code private}, {@code archive}) are {@link #isBuiltIn() protected from
 * deletion}.
 *
 * @since TODO
 */
@NullMarked
public interface NavigatorCategory {

    /**
     * Gets the stable, lower-case identifier of this category (e.g. {@code "public"}). Unique within the
     * {@link NavigatorCategoryRegistry}; this is what folders and the navigator persist.
     *
     * @return The category identifier
     */
    String getId();

    /**
     * Gets the configurable display name of this category, with legacy colour codes unresolved.
     *
     * @return The display name
     */
    String getDisplayName();

    /**
     * Gets the legacy colour-code token (e.g. {@code "&b"}) applied to this category in the navigator.
     *
     * @return The colour token
     */
    String getColor();

    /**
     * Gets the material used to represent this category in the navigator and setup menus.
     *
     * @return The icon material
     */
    XMaterial getIcon();

    /**
     * Gets the skull texture applied when this category's {@link #getIcon() icon} is a player head. Returns
     * {@code null} when none is set; the literal {@code "%viewer%"} means the viewing player's own head.
     *
     * @return The skull texture, {@code "%viewer%"}, or {@code null}
     */
    @Nullable String getIconSkullTexture();

    /**
     * Gets the world visibilities this category groups. A world is eligible for the category only when its own
     * {@link Visibility} is contained here; listing both {@link Visibility#EVERYONE} and {@link Visibility#ADDED_PLAYERS}
     * (as the built-in {@code archive} category does) groups worlds of either visibility. Never empty.
     *
     * <p>Who may actually see an individual world within the category is still governed per-world by
     * {@link BuildWorld#getPermissions()}, ensuring players only see the worlds they have permission to enter.
     *
     * @return The unmodifiable, non-empty set of grouped visibilities
     */
    @Unmodifiable
    Set<Visibility> getVisibilities();

    /**
     * Gets the representative visibility used when this category needs a single one — e.g. deciding the visibility of a
     * world created from the category's menu. Prefers {@link Visibility#EVERYONE} when the category groups both.
     *
     * @return The primary visibility
     */
    default Visibility getPrimaryVisibility() {
        return getVisibilities().contains(Visibility.EVERYONE) ? Visibility.EVERYONE : Visibility.ADDED_PLAYERS;
    }

    /**
     * Gets the ordered ids of the statuses grouped by this category. Statuses are shared and may be listed by several
     * categories; the world's visibility disambiguates which category it is displayed in.
     *
     * @return An unmodifiable, ordered list of status ids
     */
    @Unmodifiable
    List<String> getStatusIds();

    /**
     * Gets whether this category is given a section in the navigator. When {@code false}, its worlds are only reachable
     * directly via commands or secondary menus.
     *
     * @return {@code true} if shown in the navigator, otherwise {@code false}
     */
    boolean isShownInNavigator();

    /**
     * Gets the default navigator slot used when a layout is generated for this category.
     *
     * @return The navigator slot
     */
    int getNavigatorSlot();

    /**
     * Gets whether this is a built-in category. Built-in categories can be restyled but never deleted.
     *
     * @return {@code true} if built-in, otherwise {@code false}
     */
    boolean isBuiltIn();
}
