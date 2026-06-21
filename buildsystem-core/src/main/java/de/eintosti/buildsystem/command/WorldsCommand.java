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
package de.eintosti.buildsystem.command;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.command.subcommand.worlds.*;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldsCommand extends CommandBase {

    // As the composition root for the worlds subcommands, WorldsCommand keeps the plugin to resolve each subcommand's
    // collaborators; the subcommands themselves no longer depend on it.
    private final BuildSystemPlugin plugin;
    private final SubCommandDispatcher dispatcher;

    public WorldsCommand(BuildSystemPlugin plugin) {
        super(plugin.getMessages(), plugin.getLogger(), true);
        this.plugin = plugin;

        Messages messages = plugin.getMessages();
        WorldServiceImpl worldService = plugin.getWorldService();
        Menus menus = plugin.getMenus();
        Prompts prompts = plugin.getPrompts();
        MenuItems menuItems = plugin.getMenuItems();
        ConfigService configService = plugin.getConfigService();
        SettingsService settingsService = plugin.getSettingsService();
        PlayerLookupService playerLookupService = plugin.getPlayerLookupService();
        NavigatorCategoryRegistryImpl navigatorCategoryRegistry = plugin.getNavigatorCategoryRegistry();
        BackupServiceImpl backupService = plugin.getBackupService();
        Logger logger = plugin.getLogger();
        File dataFolder = plugin.getDataFolder();
        TaskScheduler scheduler = new TaskScheduler(plugin);

        this.dispatcher = new SubCommandDispatcher(
                messages,
                List.of(
                        new AddBuilderSubCommand(
                                messages, worldService, menus, playerLookupService, prompts, scheduler),
                        new BackupsSubCommand(messages, worldService, backupService, menus),
                        new BuildersSubCommand(messages, worldService, menus),
                        new DeleteSubCommand(messages, worldService, configService, menus),
                        new EditSubCommand(messages, worldService, menus),
                        new FolderSubCommand(messages, worldService, navigatorCategoryRegistry, prompts),
                        new HelpSubCommand(messages, logger),
                        new ImportAllSubCommand(messages, worldService, playerLookupService, scheduler),
                        new ImportSubCommand(messages, worldService, configService, playerLookupService, scheduler),
                        new InfoSubCommand(messages, worldService),
                        new ItemSubCommand(messages, worldService, menuItems),
                        new RemoveBuilderSubCommand(messages, worldService, playerLookupService, prompts, scheduler),
                        new RemoveSpawnSubCommand(messages, worldService),
                        new RenameSubCommand(messages, worldService, prompts),
                        new SaveTemplateSubCommand(
                                messages, worldService, configService, dataFolder, logger, scheduler),
                        new SetCreatorSubCommand(
                                messages, worldService, playerLookupService, prompts, settingsService, scheduler),
                        new SetItemSubCommand(messages, worldService),
                        new SetPermissionSubCommand(
                                messages, worldService, configService, menus, prompts, settingsService),
                        new SetProjectSubCommand(messages, worldService, menus, prompts, settingsService),
                        new SetSpawnSubCommand(messages, worldService),
                        new SetStatusSubCommand(messages, worldService, menus),
                        new TeleportSubCommand(messages, worldService),
                        new UnimportSubCommand(messages, worldService)),
                // Category shortcuts (/worlds <category>) are derived from the navigator categories; the static
                // subcommands above are registered first so a category named like a real subcommand never shadows it.
                new CategoryShortcuts(plugin));
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (!requirePermission(player, "buildsystem.navigator")) {
                return;
            }
            plugin.getMenus().openNavigator(player);
            XSound.BLOCK_CHEST_OPEN.play(player);
            return;
        }
        dispatcher.dispatch(player, args);
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        return dispatcher.complete(player, args);
    }
}
