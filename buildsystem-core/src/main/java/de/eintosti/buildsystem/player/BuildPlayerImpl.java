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
package de.eintosti.buildsystem.player;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.UUID;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * BuildSystem's only {@link BuildPlayer} implementation. Beyond the public API ({@code getUniqueId()},
 * {@code getSettings()}) it carries the runtime state core needs but does not expose: cached gameplay values for build
 * mode, the logout location, the previous teleport location, and the navigator category last opened.
 *
 * <p>Since every {@link BuildPlayer} returned by the registry is a {@code BuildPlayerImpl} at runtime, core code that
 * needs the runtime state casts via {@link #of(BuildPlayer)} or declares the local as {@code BuildPlayerImpl}. The cast
 * is type-safe: nothing else implements {@link BuildPlayer}.
 */
@NullMarked
public final class BuildPlayerImpl implements BuildPlayer {

    private final UUID uuid;
    private final Settings settings;
    private final CachedValues cachedValues;

    @Nullable private LogoutLocation logoutLocation;

    @Nullable private Location previousLocation;

    @Nullable private NavigatorCategory lastLookedAt;

    public BuildPlayerImpl(UUID uuid, Settings settings) {
        this.uuid = uuid;
        this.settings = settings;
        this.cachedValues = new CachedValues();
    }

    /**
     * Widens an API-typed reference to its impl. The cast is safe: {@code BuildPlayerImpl} is the only implementation
     * of {@link BuildPlayer}.
     *
     * @param buildPlayer The API view
     * @return The same instance, typed as the impl
     */
    @Nullable public static BuildPlayerImpl of(@Nullable BuildPlayer buildPlayer) {
        if (buildPlayer == null) {
            return null;
        }
        return (BuildPlayerImpl) buildPlayer;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    public CachedValues getCachedValues() {
        return cachedValues;
    }

    @Nullable public LogoutLocation getLogoutLocation() {
        return logoutLocation;
    }

    public void setLogoutLocation(@Nullable LogoutLocation logoutLocation) {
        this.logoutLocation = logoutLocation;
    }

    @Nullable public Location getPreviousLocation() {
        return previousLocation;
    }

    public void setPreviousLocation(@Nullable Location location) {
        this.previousLocation = location;
    }

    @Nullable public NavigatorCategory getLastLookedAt() {
        return lastLookedAt;
    }

    public void setLastLookedAt(@Nullable NavigatorCategory type) {
        this.lastLookedAt = type;
    }
}
