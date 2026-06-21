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
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.command.PluginCommand;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class CommandRegistrar {

    private final BuildSystemPlugin plugin;
    private final Services services;

    public CommandRegistrar(BuildSystemPlugin plugin, Services services) {
        this.plugin = plugin;
        this.services = services;
    }

    public void registerAll() {
        Messages messages = services.messages();
        Logger logger = plugin.getLogger();
        Menus menus = services.menus();
        WorldStorageImpl worldStorage = services.world().getWorldStorage();

        register("back", new BackCommand(messages, logger, services.player().getPlayerStorage()));
        register("blocks", new BlocksCommand(messages, logger, menus));
        register("build", new BuildCommand(messages, logger, services.player()));
        register("buildsystem", new BuildSystemCommand(messages, logger));
        register("config", new ConfigCommand(messages, logger, plugin));
        register("explosions", new ExplosionsCommand(messages, logger, worldStorage));
        register("gamemode", new GamemodeCommand(messages, logger));
        register("noai", new NoAICommand(messages, logger, worldStorage));
        register("physics", new PhysicsCommand(messages, logger, worldStorage));
        register("settings", new SettingsCommand(messages, logger, menus));
        register("setup", new SetupCommand(messages, logger, menus));
        register("skull", new SkullCommand(messages, logger));
        register("spawn", new SpawnCommand(messages, logger, services.config(), services.spawn(), worldStorage));
        register("speed", new SpeedCommand(messages, logger, menus));
        TimeCommand timeCommand = new TimeCommand(messages, logger, services.config(), worldStorage);
        register("day", timeCommand);
        register("night", timeCommand);
        register("top", new TopCommand(messages, logger));
        register("worlds", new WorldsCommand(plugin, services));
    }

    private void register(String name, CommandBase command) {
        PluginCommand cmd =
                Objects.requireNonNull(plugin.getCommand(name), "Missing plugin.yml entry for command: " + name);
        cmd.setExecutor(command);
        cmd.setTabCompleter(command);
    }
}
