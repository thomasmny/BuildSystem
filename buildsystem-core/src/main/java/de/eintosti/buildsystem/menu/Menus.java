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
package de.eintosti.buildsystem.menu;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.player.customblock.CustomBlockMenu;
import de.eintosti.buildsystem.player.menu.DesignMenu;
import de.eintosti.buildsystem.player.menu.SettingsMenu;
import de.eintosti.buildsystem.player.menu.SpeedMenu;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.menu.BackupsConfirmationMenu;
import de.eintosti.buildsystem.world.menu.BackupsMenu;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Factory and navigation hub for the plugin's GUIs. As the composition root for the menu layer it resolves each menu's
 * collaborators and constructs it, so a menu (or command/listener) opens another menu by calling {@code openX(...)}
 * here rather than constructing it directly. This confines menu wiring to one place and — crucially given that menus
 * open one another cyclically — lets each menu depend only on its own collaborators plus this hub, instead of the
 * compounding constructor parameters that direct construction would force.
 */
@NullMarked
public final class Menus {

    private final BuildSystemPlugin plugin;
    private final TaskScheduler scheduler;

    public Menus(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new TaskScheduler(plugin);
    }

    public void openSpeed(Player player) {
        new SpeedMenu(plugin.getMessages(), plugin.getSettingsService(), player).open(player);
    }

    public void openBlocks(Player player) {
        new CustomBlockMenu(plugin.getMessages(), plugin.getMenuItems(), player).open(player);
    }

    public void openDesign(Player player) {
        new DesignMenu(plugin.getMessages(), plugin.getSettingsService(), plugin.getMenuItems(), this, player)
                .open(player);
    }

    public void openSettings(Player player) {
        new SettingsMenu(
                        plugin.getMessages(),
                        plugin.getSettingsService(),
                        plugin.getConfigService(),
                        plugin.getMenuItems(),
                        plugin.getNavigatorService(),
                        plugin.getNoClipService(),
                        this,
                        player)
                .open(player);
    }

    public void openBackups(BuildWorld buildWorld, Player player) {
        new BackupsMenu(
                        plugin.getMessages(),
                        plugin.getBackupService(),
                        plugin.getMenuItems(),
                        plugin.getConfigService(),
                        plugin.getLogger(),
                        scheduler,
                        this,
                        buildWorld,
                        player)
                .open(player);
    }

    public void openBackupsConfirmation(Backup backup, Player player) {
        new BackupsConfirmationMenu(plugin.getMessages(), plugin.getConfigService(), backup, player).open(player);
    }
}
