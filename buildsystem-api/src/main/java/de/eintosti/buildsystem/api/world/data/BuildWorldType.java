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
package de.eintosti.buildsystem.api.world.data;

import org.bukkit.World.Environment;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

public enum BuildWorldType {

    /**
     * The equivalent to a default Minecraft world with {@link Environment#NORMAL}.
     */
    NORMAL("type_normal"),

    /**
     * The equivalent to a super-flat Minecraft world.
     */
    FLAT("type_flat"),

    /**
     * The equivalent to a default Minecraft world with {@link Environment#NETHER}.
     */
    NETHER("type_nether"),

    /**
     * The equivalent to a default Minecraft world with {@link Environment#THE_END}.
     */
    END("type_end"),

    /**
     * A completely empty world with no blocks at all, except the block a player spawns on.
     */
    VOID("type_void"),

    /**
     * A world which is an identical copy of a provided template.
     */
    TEMPLATE("type_template"),

    /**
     * A world which by default cannot be modified by any player except for the creator.
     */
    PRIVATE("type_private"),

    /**
     * A world which was not created by BuildSystem but was imported, so it can be used by the plugin.
     */
    IMPORTED(null),

    /**
     * A world with a custom chunk generator
     */
    CUSTOM("type_custom"),

    /**
     * A world with an unknown type.
     */
    UNKNOWN(null);

    private final String messageKey;

    BuildWorldType(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Get the message key for the {@link BuildWorldType}'s display name.
     *
     * @return The type's display name message key, or {@code null} for {@link #IMPORTED} and {@link #UNKNOWN}
     */
    @Internal
    @Nullable
    public String getMessageKey() {
        return messageKey;
    }
}