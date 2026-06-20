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
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.integration.axiom.WorldManipulateByAxiomListener;
import de.eintosti.buildsystem.integration.worldedit.EditSessionListener;
import de.eintosti.buildsystem.listener.color.AsyncPlayerChatListener;
import de.eintosti.buildsystem.listener.color.SignChangeListener;
import de.eintosti.buildsystem.listener.navigator.InventoryCreativeListener;
import de.eintosti.buildsystem.listener.navigator.NavigatorListener;
import de.eintosti.buildsystem.listener.navigator.PlayerMoveListener;
import de.eintosti.buildsystem.listener.player.*;
import de.eintosti.buildsystem.listener.settings.*;
import de.eintosti.buildsystem.listener.world.*;
import de.eintosti.buildsystem.menu.MenuListener;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.event.Listener;
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
        WorldStorageImpl worldStorage = plugin.getWorldService().getWorldStorage();
        ConfigService configService = plugin.getConfigService();
        SettingsService settingsService = plugin.getSettingsService();
        WorldStatusRegistry worldStatusRegistry = plugin.getWorldStatusRegistry();

        register(new AsyncPlayerChatListener());
        register(new AsyncPlayerPreLoginListener(plugin));
        register(new BlockPhysicsListener(worldStorage, configService));
        register(new BuildModePreventationListener(plugin));
        register(new BuildWorldResetUnloadListener(worldStorage));
        register(new DisabledInteractionsListener(plugin));
        register(new EntityDamageListener(plugin));
        register(new EntitySpawnListener(worldStorage));
        register(new FoodLevelChangeListener(worldStorage));
        register(new InstantSignPlacementListener(plugin));
        register(new InventoryCreativeListener(plugin));
        register(new IronDoorListener(plugin));
        register(new MenuListener());
        register(new PlayerChatInput.ChatInputListener());
        register(new NavigatorListener(plugin));
        register(new PlayerChangedWorldListener(plugin));
        register(new PlayerCommandPreprocessListener(plugin));
        register(new PlayerInventoryClearListener(plugin));
        register(new PlayerJoinListener(plugin));
        register(new PlayerMoveListener(plugin));
        register(new PlayerQuitListener(plugin));
        register(new PlayerRespawnListener(plugin));
        register(new PlayerTeleportListener(plugin));
        register(new PlantPlacementListener(plugin));
        register(new SignChangeListener());
        register(new SlabListener(plugin));
        register(new WeatherChangeListener(configService));
        register(new WorldManipulateListener(worldStorage, configService, worldStatusRegistry, settingsService));

        registerIntegrations(configService);
    }

    private void register(Listener listener) {
        pluginManager.registerEvents(listener, plugin);
    }

    /**
     * Registers listeners that back optional third-party integrations, each guarded by the presence of the integrated
     * plugin. {@link EditSessionListener} hooks WorldEdit's own event bus from its constructor, so it is created rather
     * than registered through the {@link PluginManager}.
     */
    private void registerIntegrations(ConfigService configService) {
        if (pluginManager.getPlugin("AxiomPaper") != null) {
            register(new WorldManipulateByAxiomListener(plugin));
        }

        boolean isWorldEdit =
                pluginManager.getPlugin("WorldEdit") != null || pluginManager.getPlugin("FastAsyncWorldEdit") != null;
        if (isWorldEdit && configService.current().settings().builder().blockWorldEditNonBuilder()) {
            new EditSessionListener(plugin);
        }
    }
}
