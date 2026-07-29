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
import de.eintosti.buildsystem.config.PluginConfig.World.Defaults.Time;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class TimeCommand extends CommandBase {

    private final ConfigService configService;
    private final WorldStorageImpl worldStorage;

    public TimeCommand(Messages messages, Logger logger, ConfigService configService, WorldStorageImpl worldStorage) {
        super(messages, logger, true);
        this.configService = configService;
        this.worldStorage = worldStorage;
    }

    /**
     * The two labels this command serves. They differ only in the permission, the configured time, and the message
     * prefix, so the difference is data instead of two near-identical branches — which is how the night path came to
     * send the day path's "unknown world" message.
     */
    private enum Variant {
        DAY("day", Permissions.DAY, Time::noon),
        NIGHT("night", Permissions.NIGHT, Time::night);

        private final String label;
        private final String permission;
        private final ToIntFunction<Time> tick;

        Variant(String label, String permission, ToIntFunction<Time> tick) {
            this.label = label;
            this.permission = permission;
            this.tick = tick;
        }

        static @Nullable Variant forLabel(String label) {
            String normalized = label.toLowerCase(Locale.ROOT);
            for (Variant variant : values()) {
                if (variant.label.equals(normalized)) {
                    return variant;
                }
            }
            return null;
        }
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        Variant variant = Variant.forLabel(label);
        if (variant == null) {
            // Only reachable if a label is registered in plugin.yml but not added above; say so rather than no-op.
            logger.warning("TimeCommand received unknown label \"" + label + "\"");
            return;
        }

        String worldName = worldNameFromArgs(player, args, 0);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messages.sendMessage(player, variant.label + "_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, variant.permission)) {
            messages.sendPermissionError(player);
            return;
        }

        if (args.length > 1) {
            messages.sendMessage(player, variant.label + "_usage");
            return;
        }

        Time time = configService.current().world().defaults().time();
        world.setTime(variant.tick.applyAsInt(time));
        messages.sendMessage(player, variant.label + "_set", Placeholders.of("%world%", world.getName()));
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        String lc = label.toLowerCase(Locale.ROOT);
        switch (lc) {
            case "day":
            case "night":
                worldStorage.getBuildWorlds().stream()
                        .filter(world -> world.getPermissions().canPerformCommand(player, Permissions.command(lc)))
                        .forEach(world -> addArgument(args[0], world.getName(), list));
                break;
        }
        return list;
    }
}
