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
package de.eintosti.buildsystem.api.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a building status a {@link BuildWorld} can have, indicating the progression and accessibility of a world.
 *
 * <p>Statuses are dynamic; server administrators can create, restyle, and delete custom statuses at runtime through
 * the in-game setup menu. Instances are obtained and resolved through the {@link WorldStatusRegistry}. Six built-in
 * statuses are always present and {@link #isBuiltIn() protected from deletion}: {@code not_started}, {@code in_progress},
 * {@code almost_finished}, {@code finished}, {@code archive}, and {@code hidden}.
 *
 * <p>Behavioral traits are data-driven and defined by individual properties, such as whether building is permitted,
 * visibility flags, and automatic progression rules.
 *
 * @since TODO
 */
@NullMarked
public interface BuildWorldStatus {

    /**
     * Gets the stable, lower-case identifier of this status (e.g. {@code "in_progress"}). The id is unique within the
     * {@link WorldStatusRegistry} and is what gets persisted; it never changes once created.
     *
     * @return The status identifier
     */
    String getId();

    /**
     * Gets the configurable display name of this status, with legacy colour codes unresolved.
     *
     * @return The display name
     */
    String getDisplayName();

    /**
     * Gets the legacy colour-code token (e.g. {@code "&a"}) applied to this status and, by default, to the worlds that
     * carry it.
     *
     * @return The colour token
     */
    String getColor();

    /**
     * Gets the material used to represent this status in menus.
     *
     * @return The icon material
     */
    XMaterial getIcon();

    /**
     * Gets the display name prefixed with this status's {@link #getColor() colour}, with legacy colour codes still
     * unresolved. Callers translate the codes when rendering.
     *
     * @return The colour-prefixed display name
     */
    default String getStyledName() {
        return getColor() + getDisplayName();
    }

    /**
     * Gets the ordering weight of this status. Lower values are considered earlier in a world's lifecycle and are used
     * to order the status grid and the status-based world sort.
     *
     * @return The ordering weight
     */
    int getOrder();

    /**
     * Gets the permission required to assign this status to a world (e.g. {@code "buildsystem.setstatus.inprogress"}).
     *
     * @return The permission string
     */
    String getPermission();

    /**
     * Gets whether worlds with this status may be modified (blocks placed/broken, interactions). When {@code false},
     * modification can still be granted to users with the {@code buildsystem.bypass.archive} permission.
     *
     * @return {@code true} if building is allowed, otherwise {@code false}
     */
    boolean isBuildingAllowed();

    /**
     * Gets whether worlds with this status are shown in the navigator.
     *
     * @return {@code true} if visible in the navigator, otherwise {@code false}
     */
    boolean isVisibleInNavigator();

    /**
     * Gets the id of the status a world is automatically advanced to the first time it is modified.
     *
     * @return The target status id, or {@link Optional#empty()} if this status does not auto-advance
     */
    Optional<String> getProgressesTo();

    /**
     * Gets whether this is a built-in status. Built-in statuses can be restyled but never deleted, guaranteeing a valid
     * fallback always exists.
     *
     * @return {@code true} if built-in, otherwise {@code false}
     */
    boolean isBuiltIn();
}
