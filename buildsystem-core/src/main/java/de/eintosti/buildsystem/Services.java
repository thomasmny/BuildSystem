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
import org.jspecify.annotations.NullMarked;

/**
 * Internal holder for the plugin's services. Owns the service fields and constructs them in the exact order the plugin
 * lifecycle requires; {@link BuildSystemPlugin}'s getters delegate to this holder.
 */
@NullMarked
final class Services {

    private final BuildSystemPlugin plugin;

    private ConfigService configService;
    private Messages messages;

    private NavigatorService navigatorService;
    private CustomBlockManager customBlockManager;
    private PlayerServiceImpl playerService;
    private PlayerLookupService playerLookupService;
    private NoClipService noClipService;
    private SettingsService settingsService;
    private SpawnService spawnService;
    private WorldServiceImpl worldService;
    private BackupService backupService;
    private CustomizableIcons customizableIcons;
    private MenuItems menuItems;

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
        this.messages = new Messages(plugin, configService);
        return this.messages;
    }

    /**
     * Constructs the remaining services in the exact order required by the plugin lifecycle. Called during
     * {@code onEnable}.
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

    NavigatorService navigator() {
        return navigatorService;
    }

    CustomBlockManager customBlockManager() {
        return customBlockManager;
    }

    PlayerServiceImpl player() {
        return playerService;
    }

    PlayerLookupService playerLookup() {
        return playerLookupService;
    }

    NoClipService noClip() {
        return noClipService;
    }

    SettingsService settings() {
        return settingsService;
    }

    SpawnService spawn() {
        return spawnService;
    }

    WorldServiceImpl world() {
        return worldService;
    }

    BackupService backup() {
        return backupService;
    }

    CustomizableIcons customizableIcons() {
        return customizableIcons;
    }

    MenuItems menuItems() {
        return menuItems;
    }
}
