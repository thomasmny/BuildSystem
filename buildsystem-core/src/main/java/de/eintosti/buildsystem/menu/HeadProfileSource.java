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
package de.eintosti.buildsystem.menu;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.display.Displayable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implemented by {@link Displayable}s that can offer the menu renderer a head profile for un-textured player-head
 * icons (e.g. a world defaulting to its creator's skin). Internal to the renderer: the public API only exposes the
 * {@link Displayable#getIconSkullTexture() skull texture string}, keeping XSeries types out of it.
 */
@NullMarked
public interface HeadProfileSource {

    /**
     * Gets the profile used to render this displayable's head when its icon is a player head and no explicit skull
     * texture is configured. Returns {@code null} to render a plain, un-textured head. The renderer resolves this
     * asynchronously, so implementations may return network-backed profiles without blocking the inventory from
     * opening.
     *
     * @return The default head profile, or {@code null} for a plain head
     */
    @Nullable Profileable getHeadProfile();

    /**
     * Gets the profile the renderer falls back to when the {@link #getHeadProfile() head profile} cannot be resolved
     * (e.g. a player name that does not map to a real account). Returns {@code null} to leave a plain head when
     * resolution fails. Like the head profile, this is resolved asynchronously.
     *
     * @return The fallback head profile, or {@code null} for none
     */
    @Nullable Profileable getHeadFallbackProfile();
}
