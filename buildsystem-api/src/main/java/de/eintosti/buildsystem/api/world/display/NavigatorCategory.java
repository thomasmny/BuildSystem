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
 * <p>Server administrators can create, restyle, and delete categories at runtime through the in-game setup menu. Three
 * categories are {@link #isBuiltIn() built in} and seeded by default ({@code public}, {@code private},
 * {@code archive}) and can be restored after deletion by resetting to defaults. The registry always keeps at least one
 * category so a {@link NavigatorCategoryRegistry#getDefaultCategory() default} fallback always exists.
 *
 * <p>Two categories are equal if and only if they share the same {@link #getId() id}. Compare categories with
 * {@link Object#equals(Object) equals}, never with {@code ==}: this type is no longer an enum, so reference identity is
 * not guaranteed even for the same logical category. Implementations must define {@code equals} and {@code hashCode}
 * consistently with the id.
 *
 * @since 4.0.0
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
     * Gets the display name prefixed with this category's {@link #getColor() colour}, with legacy colour codes still
     * unresolved. Callers translate the codes when rendering.
     *
     * @return The colour-prefixed display name
     * @since 4.0.0
     */
    default String getStyledName() {
        return getColor() + getDisplayName();
    }

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
     * Gets whether this category groups a world of the given visibility and status — i.e. the category's
     * {@link #getVisibilities() visibilities} contain the visibility and its {@link #getStatusIds() statuses} contain
     * the status id. A world may be grouped by several overlapping categories, appearing in each of their navigator
     * sections.
     *
     * @param visibility The world's visibility
     * @param statusId The world's status id
     * @return {@code true} if this category groups such a world
     * @since 4.0.0
     */
    default boolean groups(Visibility visibility, String statusId) {
        return getVisibilities().contains(visibility) && getStatusIds().contains(statusId);
    }

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
     * Gets whether this category is one of the plugin's built-in defaults, as opposed to an administrator-created
     * custom category. Built-in categories can be restyled and deleted like any other; deleting one only removes it
     * until the categories are reset to their defaults.
     *
     * @return {@code true} if built-in, otherwise {@code false}
     */
    boolean isBuiltIn();
}
