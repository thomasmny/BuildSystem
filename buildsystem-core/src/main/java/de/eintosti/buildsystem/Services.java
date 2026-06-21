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
 * Internal holder for the plugin's services. Owns the service fields and constructs them in the exact order the plugin
 * lifecycle requires; {@link BuildSystemPlugin}'s getters delegate to this holder.
 */
@NullMarked
final class Services {

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

    Services(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the configuration service. Must be called first, during {@code onLoad}.
     */
    ConfigService createConfigService() {
        this.configService = new ConfigService(plugin);
        return this.configService;
    }

    /**
     * Creates the message service, which depends on the already-created {@link ConfigService}. Called during
     * {@code onLoad}.
     */
    Messages createMessages() {
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
        (this.worldService = new WorldServiceImpl(plugin, messages(), this::spawn)).init();
        this.backupService = new BackupServiceImpl(plugin);
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
        this.menus = new Menus(plugin);
        this.prompts = new Prompts(messages(), config(), new TaskScheduler(plugin));
    }

    private <T> T checkNotNull(@Nullable T service, String serviceName) {
        if (service == null) {
            throw new IllegalStateException(serviceName + " has not been initialized yet. Check the plugin lifecycle.");
        }
        return service;
    }

    ConfigService config() {
        return checkNotNull(configService, "ConfigService");
    }

    Messages messages() {
        return checkNotNull(messages, "Messages");
    }

    NavigatorService navigator() {
        return checkNotNull(navigatorService, "NavigatorService");
    }

    NavigatorEditorService navigatorEditor() {
        return checkNotNull(navigatorEditorService, "NavigatorEditorService");
    }

    CustomBlockManager customBlockManager() {
        return checkNotNull(customBlockManager, "CustomBlockManager");
    }

    PlayerServiceImpl player() {
        return checkNotNull(playerService, "PlayerServiceImpl");
    }

    PlayerLookupService playerLookup() {
        return checkNotNull(playerLookupService, "PlayerLookupService");
    }

    NoClipService noClip() {
        return checkNotNull(noClipService, "NoClipService");
    }

    SettingsService settings() {
        return checkNotNull(settingsService, "SettingsService");
    }

    SpawnService spawn() {
        return checkNotNull(spawnService, "SpawnService");
    }

    WorldServiceImpl world() {
        return checkNotNull(worldService, "WorldServiceImpl");
    }

    BackupServiceImpl backup() {
        return checkNotNull(backupService, "BackupServiceImpl");
    }

    CustomizableIcons customizableIcons() {
        return checkNotNull(customizableIcons, "CustomizableIcons");
    }

    NavigatorCategoryRegistryImpl navigatorCategoryRegistry() {
        return checkNotNull(navigatorCategoryRegistry, "NavigatorCategoryRegistryImpl");
    }

    WorldStatusRegistryImpl worldStatusRegistry() {
        return checkNotNull(worldStatusRegistry, "WorldStatusRegistryImpl");
    }

    MenuItems menuItems() {
        return checkNotNull(menuItems, "MenuItems");
    }

    Menus menus() {
        return checkNotNull(menus, "Menus");
    }

    Prompts prompts() {
        return checkNotNull(prompts, "Prompts");
    }
}
