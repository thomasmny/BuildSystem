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
package de.eintosti.buildsystem.listener.world;

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.event.player.PlayerInventoryClearEvent;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.integration.worldedit.WorldEditCommands;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerCommandPreprocessListener implements Listener {

    private final SettingsService settingsManager;
    private final WorldStorage worldStorage;
    private final MenuItems menuItems;
    private final ConfigService configService;
    private final Messages messages;
    private final TaskScheduler scheduler;
    private final WorldProtectionPolicy policy;

    public PlayerCommandPreprocessListener(
            SettingsService settingsManager,
            WorldStorage worldStorage,
            MenuItems menuItems,
            ConfigService configService,
            Messages messages,
            TaskScheduler scheduler) {
        this.settingsManager = settingsManager;
        this.worldStorage = worldStorage;
        this.menuItems = menuItems;
        this.configService = configService;
        this.messages = messages;
        this.scheduler = scheduler;
        this.policy = new WorldProtectionPolicy();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String command = event.getMessage().split(" ")[0];
        Player player = event.getPlayer();

        if (command.equalsIgnoreCase("/clear")) {
            ItemStack navigatorItem = menuItems.createNavigatorItem(player);
            if (!player.getInventory().contains(navigatorItem)) {
                return;
            }

            if (settingsManager.getSettings(player).isKeepNavigator()) {
                List<Integer> navigatorSlots = menuItems.getNavigatorSlots(player);
                scheduler.runLater(
                        () -> Bukkit.getServer()
                                .getPluginManager()
                                .callEvent(new PlayerInventoryClearEvent(player, navigatorSlots)),
                        2L);
            }
            return;
        }

        if (configService.current().settings().builder().blockWorldEditNonBuilder()) {
            if (!WorldEditCommands.RESTRICTED.contains(command)) {
                return;
            }

            BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
            if (buildWorld == null) {
                return;
            }

            if (policy.checkStatus(player, buildWorld) == Denial.STATUS_LOCKED) {
                event.setCancelled(true);
                messages.sendMessage(player, "command_archive_world");
                return;
            }

            if (policy.checkBuilders(player, buildWorld) == Denial.NOT_A_BUILDER) {
                event.setCancelled(true);
                messages.sendMessage(player, "command_not_builder");
            }
        }
    }
}
