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
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupService;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.spawn.SpawnService;

/**
 * Internal holder for the plugin's services. Centralizes the service fields and their construction order so that
 * {@link BuildSystemPlugin} can delegate its getters here instead of carrying the wiring inline.
 *
 * <p>The construction order is load-bearing: {@link #initClasses()} mirrors the order services depend on each other.
 */
final class Services {

    private final BuildSystemPlugin plugin;

    // Built during onLoad, before the onEnable-phase services.
    private ConfigService configService;
    private Messages messages;

    // Built during onEnable, in dependency order (see initClasses()).
    private CustomizableIcons customizableIcons;
    private CustomBlockManager customBlockManager;
    private PlayerLookupService playerLookupService;
    private PlayerServiceImpl playerService;
    private NavigatorService navigatorService;
    private NoClipService noClipService;
    private WorldServiceImpl worldService;
    private BackupService backupService;
    private SettingsService settingsService;
    private SpawnService spawnService;
    private MenuItems menuItems;

    Services(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Builds the config service and messages. Invoked from {@code onLoad}, before {@link #initClasses()}.
     */
    void loadCore() {
        this.configService = new ConfigService(plugin);
    }

    void loadMessages() {
        this.messages = new Messages(plugin, configService);
    }

    /**
     * Constructs the onEnable-phase services in their required order. The sequence is load-bearing.
     */
    void initClasses() {
        this.customizableIcons = new CustomizableIcons(plugin);

        this.customBlockManager = new CustomBlockManager(plugin);
        this.playerLookupService = new PlayerLookupService(plugin);
        (this.playerService = new PlayerServiceImpl(plugin)).init();
        this.navigatorService = new NavigatorService(plugin);
        this.noClipService = new NoClipService(plugin);
        (this.worldService = new WorldServiceImpl(plugin)).init();
        this.backupService = new BackupService(plugin);
        this.settingsService = new SettingsService(plugin);
        this.spawnService = new SpawnService(plugin);
        this.menuItems = new MenuItems(plugin, configService, messages, settingsService);
    }

    ConfigService config() {
        return configService;
    }

    Messages messages() {
        return messages;
    }

    CustomizableIcons customizableIcons() {
        return customizableIcons;
    }

    CustomBlockManager customBlockManager() {
        return customBlockManager;
    }

    PlayerLookupService playerLookup() {
        return playerLookupService;
    }

    PlayerServiceImpl player() {
        return playerService;
    }

    NavigatorService navigator() {
        return navigatorService;
    }

    NoClipService noClip() {
        return noClipService;
    }

    WorldServiceImpl world() {
        return worldService;
    }

    BackupService backup() {
        return backupService;
    }

    SettingsService settings() {
        return settingsService;
    }

    SpawnService spawn() {
        return spawnService;
    }

    MenuItems menuItems() {
        return menuItems;
    }
}
