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
import org.bukkit.command.PluginCommand;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class CommandRegistrar {

    private final BuildSystemPlugin plugin;

    public CommandRegistrar(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        register("back", new BackCommand(plugin));
        register("blocks", new BlocksCommand(plugin));
        register("build", new BuildCommand(plugin));
        register("buildsystem", new BuildSystemCommand(plugin));
        register("config", new ConfigCommand(plugin));
        register("explosions", new ExplosionsCommand(plugin));
        register("gamemode", new GamemodeCommand(plugin));
        register("noai", new NoAICommand(plugin));
        register("physics", new PhysicsCommand(plugin));
        register("settings", new SettingsCommand(plugin));
        register("setup", new SetupCommand(plugin));
        register("skull", new SkullCommand(plugin));
        register("spawn", new SpawnCommand(plugin));
        register("speed", new SpeedCommand(plugin));
        TimeCommand timeCommand = new TimeCommand(plugin);
        register("day", timeCommand);
        register("night", timeCommand);
        register("top", new TopCommand(plugin));
        register("worlds", new WorldsCommand(plugin));
    }

    private void register(String name, CommandBase command) {
        PluginCommand cmd =
                Objects.requireNonNull(plugin.getCommand(name), "Missing plugin.yml entry for command: " + name);
        cmd.setExecutor(command);
        cmd.setTabCompleter(command);
    }
}
