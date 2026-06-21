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
package de.eintosti.buildsystem.world;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * The collaborators a {@link BuildWorldImpl} (and its lifecycle facades and {@code FolderImpl}) need to render and
 * manage themselves, bundled as one injectable value instead of each reaching them through the {@code BuildSystemPlugin}
 * service-locator. Introducing this parameter object keeps the entity constructors small while making their real
 * dependencies explicit — the same shape used for the menu layer's {@code DisplayablesContext}.
 */
@NullMarked
public record WorldContext(
        Messages messages,
        MenuItems menuItems,
        ConfigService configService,
        PlayerServiceImpl playerService,
        SpawnService spawnService,
        WorldStatusRegistry statusRegistry,
        CustomizableIcons customizableIcons,
        TaskScheduler scheduler,
        Logger logger) {

    /**
     * Builds a context from the plugin's services. A transitional bridge for construction sites that still hold the
     * plugin (codecs, world creators); once those are themselves injected, a single context is built in the composition
     * root and this factory is removed.
     *
     * @param plugin The plugin to pull collaborators from
     * @return A context wired to the plugin's current services
     */
    public static WorldContext fromPlugin(BuildSystemPlugin plugin) {
        return new WorldContext(
                plugin.getMessages(),
                plugin.getMenuItems(),
                plugin.getConfigService(),
                plugin.getPlayerService(),
                plugin.getSpawnService(),
                plugin.getWorldStatusRegistry(),
                plugin.getCustomizableIcons(),
                new TaskScheduler(plugin),
                plugin.getLogger());
    }
}
