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
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ArchiveSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BackupsSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BuildersSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.DeleteSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.EditSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.FolderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.HelpSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportAllSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.InfoSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.PrivateSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.PublicSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RenameSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetCreatorSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetStatusSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.TeleportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.UnimportSubCommand;
import de.eintosti.buildsystem.world.menu.NavigatorInventory;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldsCommand extends CommandBase {

    private final SubCommandDispatcher dispatcher;

    public WorldsCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
        this.dispatcher = new SubCommandDispatcher(plugin.getMessages(), List.of(
                new ArchiveSubCommand(plugin),
                new AddBuilderSubCommand(plugin),
                new BackupsSubCommand(plugin),
                new BuildersSubCommand(plugin),
                new DeleteSubCommand(plugin),
                new EditSubCommand(plugin),
                new FolderSubCommand(plugin),
                new HelpSubCommand(plugin),
                new ImportAllSubCommand(plugin),
                new ImportSubCommand(plugin),
                new InfoSubCommand(plugin),
                new ItemSubCommand(plugin),
                new PrivateSubCommand(plugin),
                new PublicSubCommand(plugin),
                new RemoveBuilderSubCommand(plugin),
                new RemoveSpawnSubCommand(plugin),
                new RenameSubCommand(plugin),
                new SetCreatorSubCommand(plugin),
                new SetItemSubCommand(plugin),
                new SetPermissionSubCommand(plugin),
                new SetProjectSubCommand(plugin),
                new SetSpawnSubCommand(plugin),
                new SetStatusSubCommand(plugin),
                new TeleportSubCommand(plugin),
                new UnimportSubCommand(plugin)
        ));
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (args.length == 0) {
            if (!requirePermission(player, "buildsystem.navigator")) {
                return;
            }
            new NavigatorInventory(plugin, player).open(player);
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
