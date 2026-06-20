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
import de.eintosti.buildsystem.world.menu.NavigatorMenu;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldsCommand extends CommandBase {

    private final SubCommandDispatcher dispatcher;

    public WorldsCommand(BuildSystemPlugin plugin) {
        super(plugin, true);
        this.dispatcher = new SubCommandDispatcher(
                plugin.getMessages(),
                List.of(
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
                        new RemoveBuilderSubCommand(plugin),
                        new RemoveSpawnSubCommand(plugin),
                        new RenameSubCommand(plugin),
                        new SaveTemplateSubCommand(plugin),
                        new SetCreatorSubCommand(plugin),
                        new SetItemSubCommand(plugin),
                        new SetPermissionSubCommand(plugin),
                        new SetProjectSubCommand(plugin),
                        new SetSpawnSubCommand(plugin),
                        new SetStatusSubCommand(plugin),
                        new TeleportSubCommand(plugin),
                        new UnimportSubCommand(plugin)),
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
            new NavigatorMenu(plugin, player).open(player);
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
