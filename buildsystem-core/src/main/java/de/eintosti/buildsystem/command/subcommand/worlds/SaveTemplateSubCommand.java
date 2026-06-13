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
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SaveTemplateSubCommand extends AbstractSubCommand {

    public SaveTemplateSubCommand(BuildSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 3, "worlds_savetemplate");
        if (buildWorld == null) {
            return;
        }

        String templateName = args.length == 3 ? args[2] : buildWorld.getName();

        String invalidCharacters = plugin.getConfigService().current().world().invalidCharacters();
        if (StringCleaner.firstInvalidChar(templateName, invalidCharacters) != null) {
            messages.sendMessage(player, "worlds_savetemplate_invalid_name");
            return;
        }

        File templatesDir = new File(plugin.getDataFolder(), "templates");
        File templateDir = new File(templatesDir, templateName);
        if (StringCleaner.isPathEscape(templatesDir, templateDir)) {
            messages.sendMessage(player, "worlds_savetemplate_invalid_name");
            return;
        }

        if (templateDir.exists()) {
            messages.sendMessage(player, "worlds_savetemplate_exists", Map.entry("%template%", templateName));
            return;
        }

        File worldDir = new File(Bukkit.getWorldContainer(), buildWorld.getName());
        if (!worldDir.exists()) {
            messages.sendMessage(player, "worlds_savetemplate_no_directory");
            return;
        }

        World world = buildWorld.getWorld();
        if (world != null) {
            world.save();
        }
        messages.sendMessage(
                player,
                "worlds_savetemplate_started",
                Map.entry("%world%", buildWorld.getName()),
                Map.entry("%template%", templateName));
        CompletableFuture.runAsync(() -> FileUtils.copy(worldDir, templateDir))
                .whenComplete((ignored, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        plugin.getLogger()
                                .log(
                                        Level.SEVERE,
                                        "Failed to save template '" + templateName + "' from world "
                                                + buildWorld.getName(),
                                        throwable);
                        messages.sendMessage(
                                player, "worlds_savetemplate_error", Map.entry("%template%", templateName));
                    } else {
                        XSound.ENTITY_PLAYER_LEVELUP.play(player);
                        messages.sendMessage(
                                player, "worlds_savetemplate_finished", Map.entry("%template%", templateName));
                    }
                }));
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        WorldStorage ws = plugin.getWorldService().getWorldStorage();
        return WorldsCompletions.permittedWorldNames(player, ws, getArgument().getPermission(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.SAVE_TEMPLATE;
    }
}
