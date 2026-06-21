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
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The plugin's service registry and composition context. Owns the service fields and constructs them in the exact
 * order the plugin lifecycle requires, and is injected into the composition roots (the menu/listener/command
 * registrars and the API facade) so they resolve collaborators from here rather than through the plugin God-object.
 */
@NullMarked
public final class Services {

    private final BuildSystemPlugin plugin;

    private @Nullable ConfigService configService;
    private @Nullable Messages messages;

    private @Nullable NavigatorService navigatorService;
    private @Nullable NavigatorEditorService navigatorEditorService;
    private @Nullable CustomBlockManager customBlockManager;
    private @Nullable PlayerServiceImpl playerService;
    private @Nullable PlayerLookupService playerLookupService;
    private @Nullable NoClipService noClipService;
    private @Nullable SettingsService settingsService;
    private @Nullable SpawnService spawnService;
    private @Nullable WorldServiceImpl worldService;
    private @Nullable BackupServiceImpl backupService;
    private @Nullable CustomizableIcons customizableIcons;
    private @Nullable NavigatorCategoryRegistryImpl navigatorCategoryRegistry;
    private @Nullable WorldStatusRegistryImpl worldStatusRegistry;
    private @Nullable MenuItems menuItems;
    private @Nullable Menus menus;
    private @Nullable Prompts prompts;
    private @Nullable WorldContext worldContext;

    Services(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the configuration service. Must be called first, during {@code onLoad}.
     */
    public ConfigService createConfigService() {
        this.configService = new ConfigService(plugin);
        return this.configService;
    }

    /**
     * Creates the message service, which depends on the already-created {@link ConfigService}. Called during
     * {@code onLoad}.
     */
    public Messages createMessages() {
        this.messages = new Messages(plugin, config());
        return this.messages;
    }

    /**
     * Constructs the remaining services in the exact order required by the plugin lifecycle. Called during
     * {@code onEnable}.
     */
    void initClasses() {
        this.navigatorCategoryRegistry = new NavigatorCategoryRegistryImpl(plugin, this::world);
        this.worldStatusRegistry =
                new WorldStatusRegistryImpl(plugin, navigatorCategoryRegistry(), messages(), this::world);
        this.customizableIcons = new CustomizableIcons(plugin);

        this.customBlockManager = new CustomBlockManager(plugin, this::world);
        this.playerLookupService = new PlayerLookupService(plugin);
        (this.playerService = new PlayerServiceImpl(plugin, config(), this::world)).init();
        this.navigatorEditorService = new NavigatorEditorService();
        this.noClipService = new NoClipService(plugin);
        this.worldService = new WorldServiceImpl(plugin, this);
        this.backupService = new BackupServiceImpl(plugin, config(), messages(), world(), this::spawn);
        this.settingsService = new SettingsService(plugin, config(), messages(), player(), world());
        this.spawnService = new SpawnService(plugin, world());
        this.menuItems = new MenuItems(plugin, config(), messages(), settings());
        // Created after MenuItems (which it needs); nothing constructed earlier depends on it.
        this.navigatorService = new NavigatorService(
                navigatorCategoryRegistry(),
                config(),
                menuItems(),
                player(),
                messages(),
                new TaskScheduler(plugin),
                new NamespacedKey(plugin, "owner"),
                new NamespacedKey(plugin, "category"));
        this.menus = new Menus(plugin, this);
        this.prompts = new Prompts(messages(), config(), new TaskScheduler(plugin));

        // Load persisted worlds/folders last: world entities pull collaborators from a WorldContext that bundles
        // services created above (e.g. MenuItems, SpawnService), so the whole service graph must exist before loading.
        this.worldService.init();
    }

    private <T> T checkNotNull(@Nullable T service, String serviceName) {
        if (service == null) {
            throw new IllegalStateException(serviceName + " has not been initialized yet. Check the plugin lifecycle.");
        }
        return service;
    }

    public ConfigService config() {
        return checkNotNull(configService, "ConfigService");
    }

    public Messages messages() {
        return checkNotNull(messages, "Messages");
    }

    public NavigatorService navigator() {
        return checkNotNull(navigatorService, "NavigatorService");
    }

    public NavigatorEditorService navigatorEditor() {
        return checkNotNull(navigatorEditorService, "NavigatorEditorService");
    }

    public CustomBlockManager customBlockManager() {
        return checkNotNull(customBlockManager, "CustomBlockManager");
    }

    public PlayerServiceImpl player() {
        return checkNotNull(playerService, "PlayerServiceImpl");
    }

    public PlayerLookupService playerLookup() {
        return checkNotNull(playerLookupService, "PlayerLookupService");
    }

    public NoClipService noClip() {
        return checkNotNull(noClipService, "NoClipService");
    }

    public SettingsService settings() {
        return checkNotNull(settingsService, "SettingsService");
    }

    public SpawnService spawn() {
        return checkNotNull(spawnService, "SpawnService");
    }

    public WorldServiceImpl world() {
        return checkNotNull(worldService, "WorldServiceImpl");
    }

    public BackupServiceImpl backup() {
        return checkNotNull(backupService, "BackupServiceImpl");
    }

    public CustomizableIcons customizableIcons() {
        return checkNotNull(customizableIcons, "CustomizableIcons");
    }

    public NavigatorCategoryRegistryImpl navigatorCategoryRegistry() {
        return checkNotNull(navigatorCategoryRegistry, "NavigatorCategoryRegistryImpl");
    }

    public WorldStatusRegistryImpl worldStatusRegistry() {
        return checkNotNull(worldStatusRegistry, "WorldStatusRegistryImpl");
    }

    public MenuItems menuItems() {
        return checkNotNull(menuItems, "MenuItems");
    }

    public Menus menus() {
        return checkNotNull(menus, "Menus");
    }

    public Prompts prompts() {
        return checkNotNull(prompts, "Prompts");
    }

    /**
     * The {@link WorldContext} bundling the collaborators world entities render and manage themselves with. Built lazily
     * and cached: it is first needed when worlds load (the last step of {@link #initClasses()}), by which point every
     * bundled service exists.
     */
    public WorldContext worldContext() {
        if (worldContext == null) {
            worldContext = new WorldContext(
                    messages(),
                    menuItems(),
                    config(),
                    player(),
                    spawn(),
                    worldStatusRegistry(),
                    customizableIcons(),
                    new TaskScheduler(plugin),
                    plugin.getLogger());
        }
        return worldContext;
    }
}
