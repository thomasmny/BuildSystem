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
package de.eintosti.buildsystem.player;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.CachedValues;
import de.eintosti.buildsystem.api.player.LogoutLocation;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;

public class BuildPlayerImpl implements BuildPlayer {

    private final UUID uuid;
    private final Settings settings;
    private final CachedValues cachedValues;

    private BuildWorld cachedWorld;
    private LogoutLocation logoutLocation;
    private Location previousLocation;
    private NavigatorCategory lastLookedAt;

    public BuildPlayerImpl(UUID uuid, Settings settings) {
        this.uuid = uuid;
        this.settings = settings;
        this.cachedValues = new CachedValuesImpl();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public CachedValues getCachedValues() {
        return cachedValues;
    }

    @Override
    @Nullable
    public LogoutLocation getLogoutLocation() {
        return logoutLocation;
    }

    @Override
    public void setLogoutLocation(LogoutLocation logoutLocation) {
        this.logoutLocation = logoutLocation;
    }

    @Override
    @Nullable
    public Location getPreviousLocation() {
        return previousLocation;
    }

    @Override
    public void setPreviousLocation(Location location) {
        this.previousLocation = location;
    }

    @Override
    @Nullable
    public NavigatorCategory getLastLookedAt() {
        return lastLookedAt;
    }

    @Override
    public void setLastLookedAt(NavigatorCategory type) {
        this.lastLookedAt = type;
    }
}