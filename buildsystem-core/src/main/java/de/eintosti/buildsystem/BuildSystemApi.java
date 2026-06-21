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
package de.eintosti.buildsystem;

import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.backup.BackupService;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildSystemApi implements BuildSystem {

    private final Services services;

    public BuildSystemApi(Services services) {
        this.services = services;
    }

    @Override
    public WorldService getWorldService() {
        return services.world();
    }

    @Override
    public PlayerService getPlayerService() {
        return services.player();
    }

    @Override
    public BackupService getBackupService() {
        return services.backup();
    }

    @Override
    public WorldStatusRegistry getStatusRegistry() {
        return services.worldStatusRegistry();
    }

    @Override
    public NavigatorCategoryRegistry getNavigatorCategoryRegistry() {
        return services.navigatorCategoryRegistry();
    }
}
