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
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.integration.axiom.WorldManipulateByAxiomListener;
import de.eintosti.buildsystem.integration.worldedit.EditSessionListener;
import de.eintosti.buildsystem.menu.MenuListener;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ListenerRegistrar {

    private final BuildSystemPlugin plugin;
    private final PluginManager pluginManager;

    public ListenerRegistrar(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.pluginManager = plugin.getServer().getPluginManager();
    }

    public void registerAll() {
        pluginManager.registerEvents(new AsyncPlayerChatListener(plugin), plugin);
        pluginManager.registerEvents(new AsyncPlayerPreLoginListener(plugin), plugin);
        pluginManager.registerEvents(new BlockPhysicsListener(plugin), plugin);
        pluginManager.registerEvents(new BuildModePreventationListener(plugin), plugin);
        pluginManager.registerEvents(new BuildWorldResetUnloadListener(plugin), plugin);
        pluginManager.registerEvents(new DisabledInteractionsListener(plugin), plugin);
        pluginManager.registerEvents(new EntityDamageListener(plugin), plugin);
        pluginManager.registerEvents(new EntitySpawnListener(plugin), plugin);
        pluginManager.registerEvents(new FoodLevelChangeListener(plugin), plugin);
        pluginManager.registerEvents(new InstantSignPlacementListener(plugin), plugin);
        pluginManager.registerEvents(new InventoryCreativeListener(plugin), plugin);
        pluginManager.registerEvents(new IronDoorListener(plugin), plugin);
        pluginManager.registerEvents(new MenuListener(), plugin);
        pluginManager.registerEvents(new NavigatorListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerChangedWorldListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerCommandPreprocessListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerInventoryClearListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerJoinListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerMoveListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerQuitListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerRespawnListener(plugin), plugin);
        pluginManager.registerEvents(new PlayerTeleportListener(plugin), plugin);
        pluginManager.registerEvents(new PlantPlacementListener(plugin), plugin);
        pluginManager.registerEvents(new SignChangeListener(plugin), plugin);
        pluginManager.registerEvents(new SlabListener(plugin), plugin);
        pluginManager.registerEvents(new WeatherChangeListener(plugin), plugin);
        pluginManager.registerEvents(new WorldManipulateListener(plugin), plugin);

        if (pluginManager.getPlugin("AxiomPaper") != null) {
            pluginManager.registerEvents(new WorldManipulateByAxiomListener(plugin), plugin);
        }

        boolean isWorldEdit =
                pluginManager.getPlugin("WorldEdit") != null || pluginManager.getPlugin("FastAsyncWorldEdit") != null;
        if (isWorldEdit
                && plugin.getConfigService().current().settings().builder().blockWorldEditNonBuilder()) {
            new EditSessionListener(plugin);
        }
    }
}
