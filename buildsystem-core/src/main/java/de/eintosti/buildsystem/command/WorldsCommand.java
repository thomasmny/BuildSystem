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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BackupsSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BuildersSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.DeleteSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.DownloadSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.EditSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.FolderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.HelpSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportAllSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.InfoSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RenameSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SaveTemplateSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetCreatorSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetStatusSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.TeleportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.UnimportSubCommand;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.FeedbackSound;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import de.eintosti.buildsystem.world.download.WorldDownloadService;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldsCommand extends CommandBase {

    // As the composition root for the worlds subcommands, WorldsCommand resolves each subcommand's collaborators from
    // the service registry; the subcommands themselves no longer depend on the plugin.
    private final Services services;
    private final SubCommandDispatcher dispatcher;

    public WorldsCommand(BuildSystemPlugin plugin, Services services) {
        super(services.messages(), plugin.getLogger(), true);
        this.services = services;

        Messages messages = services.messages();
        WorldServiceImpl worldService = services.world();
        Menus menus = services.menus();
        Prompts prompts = services.prompts();
        NavigatorItems navigatorItems = services.navigatorItems();
        ConfigService configService = services.config();
        SettingsService settingsService = services.settings();
        PlayerLookupService playerLookupService = services.playerLookup();
        NavigatorCategoryRegistryImpl navigatorCategoryRegistry = services.navigatorCategoryRegistry();
        BackupServiceImpl backupService = services.backup();
        WorldDownloadService downloadService = services.worldDownload();
        Logger logger = plugin.getLogger();
        File dataFolder = plugin.getDataFolder();
        TaskScheduler scheduler = services.scheduler();

        this.dispatcher = new SubCommandDispatcher(
                messages,
                List.of(
                        new AddBuilderSubCommand(messages, worldService, services.worldPrompts()),
                        new BackupsSubCommand(messages, worldService, backupService, menus),
                        new BuildersSubCommand(messages, worldService, menus),
                        new DeleteSubCommand(messages, worldService, configService, menus),
                        new DownloadSubCommand(messages, worldService, downloadService, scheduler, logger),
                        new EditSubCommand(messages, worldService, menus),
                        new FolderSubCommand(messages, worldService, navigatorCategoryRegistry, prompts),
                        new HelpSubCommand(messages),
                        new ImportAllSubCommand(messages, worldService, playerLookupService),
                        new ImportSubCommand(messages, worldService, configService, prompts, playerLookupService),
                        new InfoSubCommand(messages, worldService),
                        new ItemSubCommand(messages, worldService, navigatorItems),
                        new RemoveBuilderSubCommand(messages, worldService, playerLookupService, prompts),
                        new RemoveSpawnSubCommand(messages, worldService),
                        new RenameSubCommand(messages, worldService, prompts),
                        new SaveTemplateSubCommand(
                                messages, worldService, configService, dataFolder, logger, scheduler),
                        new SetCreatorSubCommand(messages, worldService, playerLookupService, prompts, settingsService),
                        new SetItemSubCommand(messages, worldService),
                        new SetPermissionSubCommand(messages, worldService, services.worldPrompts()),
                        new SetProjectSubCommand(messages, worldService, services.worldPrompts()),
                        new SetSpawnSubCommand(messages, worldService),
                        new SetStatusSubCommand(messages, worldService, menus),
                        new TeleportSubCommand(messages, worldService),
                        new UnimportSubCommand(messages, worldService)),
                // Category shortcuts (/worlds <category>) are derived from the navigator categories; the static
                // subcommands above are registered first so a category named like a real subcommand never shadows it.
                new CategoryShortcuts(services));
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (!requirePermission(player, Permissions.NAVIGATOR)) {
                return;
            }
            services.menus().openNavigator(player);
            FeedbackSound.OPEN.play(player);
            return;
        }
        dispatcher.dispatch(player, args);
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        return dispatcher.complete(player, args);
    }
}
