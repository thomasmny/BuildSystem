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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TimeCommand extends CommandBase {

    private final ConfigService configService;
    private final WorldStorageImpl worldStorage;

    public TimeCommand(Messages messages, Logger logger, ConfigService configService, WorldStorageImpl worldStorage) {
        super(messages, logger, true);
        this.configService = configService;
        this.worldStorage = worldStorage;
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        String worldName = worldNameFromArgs(player, args, 0);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messages.sendMessage(player, "day_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(world);

        switch (label.toLowerCase(Locale.ROOT)) {
            case "day" -> {
                if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, "buildsystem.day")) {
                    messages.sendPermissionError(player);
                    return;
                }

                switch (args.length) {
                    case 0, 1 -> {
                        world.setTime(configService
                                .current()
                                .world()
                                .defaults()
                                .time()
                                .noon());
                        messages.sendMessage(player, "day_set", Map.entry("%world%", world.getName()));
                    }
                    default -> messages.sendMessage(player, "day_usage");
                }
            }

            case "night" -> {
                if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, "buildsystem.night")) {
                    messages.sendPermissionError(player);
                    return;
                }

                switch (args.length) {
                    case 0, 1 -> {
                        world.setTime(configService
                                .current()
                                .world()
                                .defaults()
                                .time()
                                .night());
                        messages.sendMessage(player, "night_set", Map.entry("%world%", world.getName()));
                    }
                    default -> messages.sendMessage(player, "night_usage");
                }
            }
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        String lc = label.toLowerCase(Locale.ROOT);
        switch (lc) {
            case "day":
            case "night":
                worldStorage.getBuildWorlds().stream()
                        .filter(world -> world.getPermissions().canPerformCommand(player, "buildsystem." + lc))
                        .forEach(world -> addArgument(args[0], world.getName(), list));
                break;
        }
        return list;
    }
}
