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
 * <p>Statuses are dynamic; server administrators can create, restyle, and delete them at runtime through the in-game
 * setup menu. Instances are obtained and resolved through the {@link WorldStatusRegistry}. Six statuses are
 * {@link #isBuiltIn() built in} and seeded by default — {@code not_started}, {@code in_progress},
 * {@code almost_finished}, {@code finished}, {@code archive}, and {@code hidden} — and can be restored after deletion
 * by resetting to defaults. The registry always keeps at least one status so a {@link WorldStatusRegistry#getDefaultStatus()
 * default} fallback always exists.
 *
 * <p>Behavioral traits are data-driven and defined by individual properties, such as whether building is permitted,
 * visibility flags, and automatic progression rules.
 *
 * <p>Two statuses are equal if and only if they share the same {@link #getId() id}. Compare statuses with
 * {@link Object#equals(Object) equals}, never with {@code ==}: this type is no longer an enum, so reference identity is
 * not guaranteed even for the same logical status. Implementations must define {@code equals} and {@code hashCode}
 * consistently with the id.
 *
 * @since 4.0.0
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
     * Gets the id of the status a world is automatically advanced to the first time it is modified.
     *
     * @return The target status id, or {@link Optional#empty()} if this status does not auto-advance
     */
    Optional<String> getProgressesTo();

    /**
     * Gets the slot this status occupies in the {@code /worlds setStatus} picker. The layout is configured through the
     * setup menu's status editor, exactly as navigator categories are arranged. A status that is not
     * {@link #isShownInStatusMenu() shown}, or whose slot falls outside the picker, is omitted from it.
     *
     * @return The picker slot, or a negative value when the status has no assigned slot
     */
    int getStatusSlot();

    /**
     * Gets whether this status appears in the {@code /worlds setStatus} picker. A hidden status still exists and can be
     * assigned through other means; it is simply not offered in the picker until it is placed back into the layout.
     *
     * @return {@code true} if shown in the picker, otherwise {@code false}
     */
    boolean isShownInStatusMenu();

    /**
     * Gets whether this status is one of the plugin's built-in defaults, as opposed to an administrator-created custom
     * status. Built-in statuses can be restyled and deleted like any other; deleting one only removes it until the
     * statuses are reset to their defaults.
     *
     * @return {@code true} if built-in, otherwise {@code false}
     */
    boolean isBuiltIn();
}
