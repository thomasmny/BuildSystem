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
package de.eintosti.buildsystem.apitest;

import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.BuildSystemProvider;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A throwaway downstream consumer of the BuildSystem API. It exists so the CI build compiles real third-party usage
 * against {@code buildsystem-api}: any breaking change to the public surface fails the build here rather than silently
 * shipping. The {@code onEnable} calls are read-only and run only if the plugin is actually loaded on a server.
 */
public final class ApiConsumerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        BuildSystem api = BuildSystemProvider.get();
        readWorlds(api.getWorldService());
        readRegistries(api.getStatusRegistry(), api.getNavigatorCategoryRegistry());

        // Touch the remaining facade services so a change to their signatures breaks compilation here too.
        api.getPlayerService();
        api.getBackupService();

        getLogger().info("BuildSystem API surface resolved successfully");
    }

    private void readWorlds(WorldService worldService) {
        worldService.getFolderStorage();
        for (BuildWorld world : worldService.getWorldStorage().getBuildWorlds()) {
            WorldData data = world.getData();
            String permission = data.get(WorldDataKey.PERMISSION);
            boolean blockBreaking = data.get(WorldDataKey.BLOCK_BREAKING);
            getLogger()
                    .fine(world.getName() + ": permission=" + permission + ", blockBreaking=" + blockBreaking
                            + ", status=" + data.get(WorldDataKey.STATUS).getId());
        }
    }

    private void readRegistries(WorldStatusRegistry statusRegistry, NavigatorCategoryRegistry categoryRegistry) {
        getLogger()
                .fine("statuses=" + statusRegistry.getStatuses().size() + " default="
                        + statusRegistry.getDefaultStatus().getId());
        getLogger()
                .fine("categories=" + categoryRegistry.getCategories().size() + " default="
                        + categoryRegistry.getDefaultCategory().getId());
    }
}
