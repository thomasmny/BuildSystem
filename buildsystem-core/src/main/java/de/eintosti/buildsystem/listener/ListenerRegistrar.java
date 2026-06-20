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
import de.eintosti.buildsystem.i18n.Messages;
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
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.MenuListener;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
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
        CustomBlockManager customBlockManager = plugin.getCustomBlockManager();
        MenuItems menuItems = plugin.getMenuItems();
        Messages messages = plugin.getMessages();
        PlayerServiceImpl playerService = plugin.getPlayerService();
        NavigatorService navigatorService = plugin.getNavigatorService();
        NavigatorEditorService navigatorEditorService = plugin.getNavigatorEditorService();
        NoClipService noClipService = plugin.getNoClipService();
        SpawnService spawnService = plugin.getSpawnService();

        register(new AsyncPlayerChatListener());
        register(new AsyncPlayerPreLoginListener(plugin));
        register(new BlockPhysicsListener(worldStorage, configService));
        register(new BuildModePreventationListener(playerService, configService));
        register(new BuildWorldResetUnloadListener(worldStorage));
        register(new DisabledInteractionsListener(customBlockManager, settingsService, worldStorage, configService));
        register(new EntityDamageListener(configService, worldStorage));
        register(new EntitySpawnListener(worldStorage));
        register(new FoodLevelChangeListener(worldStorage));
        register(new InstantSignPlacementListener(customBlockManager, settingsService, worldStorage));
        register(new InventoryCreativeListener(plugin));
        register(new IronDoorListener(settingsService, worldStorage));
        register(new MenuListener());
        register(new PlayerChatInput.ChatInputListener());
        register(new NavigatorListener(plugin));
        register(new PlayerChangedWorldListener(
                navigatorService, playerService, settingsService, worldStorage, configService, messages));
        register(new PlayerCommandPreprocessListener(plugin));
        register(new PlayerInventoryClearListener(settingsService, menuItems));
        register(new PlayerJoinListener(plugin));
        register(new PlayerMoveListener(plugin));
        register(new PlayerQuitListener(
                playerService,
                navigatorService,
                navigatorEditorService,
                noClipService,
                settingsService,
                configService,
                messages));
        register(new PlayerRespawnListener(settingsService, spawnService));
        register(new PlayerTeleportListener(messages, playerService.getPlayerStorage(), worldStorage));
        register(new PlantPlacementListener(settingsService, worldStorage));
        register(new SignChangeListener());
        register(new SlabListener(settingsService, worldStorage));
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
