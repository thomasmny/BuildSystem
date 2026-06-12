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
import de.eintosti.buildsystem.menu.MenuListener;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ListenerRegistrar {

    private final BuildSystemPlugin plugin;
    private final PluginManager pm;

    public ListenerRegistrar(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    public void registerAll() {
        pm.registerEvents(new AsyncPlayerChatListener(plugin), plugin);
        pm.registerEvents(new AsyncPlayerPreLoginListener(plugin), plugin);
        pm.registerEvents(new BlockPhysicsListener(plugin), plugin);
        pm.registerEvents(new BuildModePreventationListener(plugin), plugin);
        pm.registerEvents(new BuildWorldResetUnloadListener(plugin), plugin);
        pm.registerEvents(new DisabledInteractionsListener(plugin), plugin);
        pm.registerEvents(new EntityDamageListener(plugin), plugin);
        pm.registerEvents(new EntitySpawnListener(plugin), plugin);
        pm.registerEvents(new FoodLevelChangeListener(plugin), plugin);
        pm.registerEvents(new InstantSignPlacementListener(plugin), plugin);
        pm.registerEvents(new InventoryCreativeListener(plugin), plugin);
        pm.registerEvents(new IronDoorListener(plugin), plugin);
        pm.registerEvents(new MenuListener(), plugin);
        pm.registerEvents(new NavigatorListener(plugin), plugin);
        pm.registerEvents(new PlayerChangedWorldListener(plugin), plugin);
        pm.registerEvents(new PlayerCommandPreprocessListener(plugin), plugin);
        pm.registerEvents(new PlayerInventoryClearListener(plugin), plugin);
        pm.registerEvents(new PlayerJoinListener(plugin), plugin);
        pm.registerEvents(new PlayerMoveListener(plugin), plugin);
        pm.registerEvents(new PlayerQuitListener(plugin), plugin);
        pm.registerEvents(new PlayerRespawnListener(plugin), plugin);
        pm.registerEvents(new PlayerTeleportListener(plugin), plugin);
        pm.registerEvents(new PlantPlacementListener(plugin), plugin);
        pm.registerEvents(new SignChangeListener(plugin), plugin);
        pm.registerEvents(new SlabListener(plugin), plugin);
        pm.registerEvents(new WeatherChangeListener(plugin), plugin);
        pm.registerEvents(new WorldManipulateListener(plugin), plugin);

        if (pm.getPlugin("AxiomPaper") != null) {
            pm.registerEvents(new WorldManipulateByAxiomListener(plugin), plugin);
        }

        boolean isWorldEdit = pm.getPlugin("WorldEdit") != null || pm.getPlugin("FastAsyncWorldEdit") != null;
        if (isWorldEdit && plugin.getConfigService().current().settings().builder().blockWorldEditNonBuilder()) {
            new EditSessionListener(plugin);
        }
    }
}
