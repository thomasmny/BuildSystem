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

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

/**
 * The registry of all {@link NavigatorCategory navigator categories} known to the server, both the built-in defaults
 * and any custom categories created by administrators.
 *
 * @since TODO
 */
@NullMarked
public interface NavigatorCategoryRegistry {

    /**
     * The id of the built-in {@code public} category (everyone, shown in the navigator).
     */
    String PUBLIC_ID = "public";

    /**
     * The id of the built-in {@code archive} category (everyone, building disabled via the archive status).
     */
    String ARCHIVE_ID = "archive";

    /**
     * The id of the built-in {@code private} category (creator and builders only).
     */
    String PRIVATE_ID = "private";

    /**
     * Gets all registered categories, ordered by {@link NavigatorCategory#getNavigatorSlot()}.
     *
     * @return An unmodifiable view of all categories
     */
    @Unmodifiable
    Collection<NavigatorCategory> getCategories();

    /**
     * Resolves a category by its {@link NavigatorCategory#getId() id}.
     *
     * @param id The category id
     * @return The matching category, or {@link Optional#empty()} if none is registered with that id
     */
    Optional<NavigatorCategory> getCategory(String id);

    /**
     * Resolves the category a world is displayed in by matching both the world's visibility and its status: the world
     * belongs to the first category whose {@link NavigatorCategory#getVisibilities() visibilities} contain the world's
     * and whose {@link NavigatorCategory#getStatusIds() statuses} contain the world's status.
     *
     * @param world The world whose category to resolve
     * @return The resolved category, or the {@link #getDefaultCategory() default} when nothing matches
     */
    NavigatorCategory getCategoryForWorld(BuildWorld world);

    /**
     * Gets the default category worlds fall back to (the built-in {@code public}). Always present.
     *
     * @return The default category
     */
    NavigatorCategory getDefaultCategory();
}
