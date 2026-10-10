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

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.WorldPrompts;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import de.eintosti.buildsystem.world.download.WorldDownloadService;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;

/**
 * The plugin's service registry and composition context. Constructs the services in the order the plugin lifecycle
 * requires, handing each the collaborators it uses, and is injected into the composition roots (the menu/listener/
 * command registrars and the API facade) so they resolve collaborators from here rather than through the plugin
 * God-object.
 *
 * <p>The world and folder storages come first, since most services only need them rather than the world service.
 */
@NullMarked
public final class Services {

    private final TaskScheduler taskScheduler;
    private final ConfigService configService;
    private final Messages messages;

    private final PlayerLookupService playerLookupService;
    private final WorldStorageImpl worldStorage;
    private final FolderStorageImpl folderStorage;
    private final NavigatorCategoryRegistryImpl navigatorCategoryRegistry;
    private final WorldStatusRegistryImpl worldStatusRegistry;
    private final CustomizableIcons customizableIcons;
    private final CustomBlockManager customBlockManager;
    private final PlayerServiceImpl playerService;
    private final NavigatorEditorService navigatorEditorService;
    private final NoClipService noClipService;
    private final SpawnService spawnService;
    private final WorldOperations worldOperations;
    private final BackupServiceImpl backupService;
    private final WorldDownloadService worldDownloadService;
    private final SettingsService settingsService;
    private final MenuItems menuItems;
    private final NavigatorItems navigatorItems;
    private final NavigatorService navigatorService;
    private final Prompts prompts;
    private final WorldPrompts worldPrompts;
    private final WorldContext worldContext;
    private final WorldServiceImpl worldService;
    private final Menus menus;

    /**
     * Constructs every service. Called during {@code onEnable}, with the configuration and messages loaded in
     * {@code onLoad}. The stored worlds and folders are loaded separately, by {@link #loadWorlds()}.
     */
    Services(BuildSystemPlugin plugin, TaskScheduler taskScheduler, ConfigService configService, Messages messages) {
        this.taskScheduler = taskScheduler;
        this.configService = configService;
        this.messages = messages;

        this.playerLookupService =
                new PlayerLookupService(plugin, taskScheduler.background(), taskScheduler.mainThread());
        // The storages read the world context and category registry from here only once the stored worlds load.
        this.worldStorage = new WorldStorageImpl(plugin, this);
        this.folderStorage = new FolderStorageImpl(plugin, worldStorage, this);

        this.navigatorCategoryRegistry = new NavigatorCategoryRegistryImpl(plugin, folderStorage);
        this.worldStatusRegistry =
                new WorldStatusRegistryImpl(plugin, navigatorCategoryRegistry, messages, worldStorage);
        this.customizableIcons = new CustomizableIcons(plugin);

        this.customBlockManager = new CustomBlockManager(plugin, taskScheduler, worldStorage);
        (this.playerService = new PlayerServiceImpl(plugin, configService, worldStorage, taskScheduler)).init();
        this.navigatorEditorService = new NavigatorEditorService();
        this.noClipService = new NoClipService(taskScheduler);
        this.spawnService = new SpawnService(plugin, worldStorage, taskScheduler);
        this.worldOperations = new WorldOperations(messages, spawnService);
        this.backupService =
                new BackupServiceImpl(plugin, taskScheduler, configService, messages, worldStorage, worldOperations);
        this.worldDownloadService = new WorldDownloadService(
                configService, taskScheduler, worldOperations, plugin.getLogger(), plugin.getDataFolder());
        this.settingsService =
                new SettingsService(plugin, taskScheduler, configService, messages, playerService, worldStorage);
        this.menuItems = new MenuItems(plugin, taskScheduler, messages, settingsService);
        this.navigatorItems = new NavigatorItems(plugin, configService, messages);
        this.navigatorService = new NavigatorService(
                navigatorCategoryRegistry,
                configService,
                navigatorItems,
                menuItems,
                playerService,
                messages,
                taskScheduler,
                new NamespacedKey(plugin, "owner"),
                new NamespacedKey(plugin, "category"));
        this.prompts = new Prompts(messages, configService, taskScheduler);
        this.worldPrompts = new WorldPrompts(messages, prompts, settingsService, configService, playerLookupService);
        this.worldContext = new WorldContext(
                messages,
                menuItems,
                configService,
                playerService,
                spawnService,
                worldStatusRegistry,
                customizableIcons,
                taskScheduler,
                plugin.getLogger(),
                worldOperations);
        this.worldService = new WorldServiceImpl(plugin, this, worldStorage, folderStorage);
        this.menus = new Menus(plugin, this);
    }

    /**
     * Loads the stored folders and worlds. Last, since world entities pull collaborators from the {@link WorldContext}.
     */
    void loadWorlds() {
        this.worldService.init();
    }

    /**
     * The plugin's single {@link TaskScheduler}, owning the shared background executor. Available for the whole plugin
     * lifetime; {@link TaskScheduler#shutdown() shut down} on disable.
     */
    public TaskScheduler scheduler() {
        return taskScheduler;
    }

    public ConfigService config() {
        return configService;
    }

    public Messages messages() {
        return messages;
    }

    public NavigatorService navigator() {
        return navigatorService;
    }

    public NavigatorEditorService navigatorEditor() {
        return navigatorEditorService;
    }

    public PlayerServiceImpl player() {
        return playerService;
    }

    public PlayerLookupService playerLookup() {
        return playerLookupService;
    }

    public NoClipService noClip() {
        return noClipService;
    }

    public SettingsService settings() {
        return settingsService;
    }

    public SpawnService spawn() {
        return spawnService;
    }

    public WorldServiceImpl world() {
        return worldService;
    }

    public WorldOperations operations() {
        return worldOperations;
    }

    public BackupServiceImpl backup() {
        return backupService;
    }

    public WorldDownloadService worldDownload() {
        return worldDownloadService;
    }

    public CustomizableIcons customizableIcons() {
        return customizableIcons;
    }

    public NavigatorCategoryRegistryImpl navigatorCategoryRegistry() {
        return navigatorCategoryRegistry;
    }

    public WorldStatusRegistryImpl worldStatusRegistry() {
        return worldStatusRegistry;
    }

    public MenuItems menuItems() {
        return menuItems;
    }

    public NavigatorItems navigatorItems() {
        return navigatorItems;
    }

    public Menus menus() {
        return menus;
    }

    public Prompts prompts() {
        return prompts;
    }

    /**
     * {@return the world chat prompts the editor menus and the {@code /worlds} subcommands share}
     */
    public WorldPrompts worldPrompts() {
        return worldPrompts;
    }

    /**
     * {@return the collaborators world entities render and manage themselves with}
     */
    public WorldContext worldContext() {
        return worldContext;
    }
}
