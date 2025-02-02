/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.util;

import de.eintosti.buildsystem.BuildSystem;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class to determine the mode in which the server is running. The server can be in one of the following modes:
 * <ul>
 *     <li>{@link ServerMode#ONLINE} - The server is running in online mode</li>
 *     <li>{@link ServerMode#PROXY} - The server is running behind a proxy like BungeeCord or Velocity</li>
 *     <li>{@link ServerMode#OFFLINE} - The server is running in offline mode</li>
 * </ul>
 */
public final class ServerModeChecker {

    private static final ServerMode SERVER_MODE = determineServerMode();

    private ServerModeChecker() {
    }

    /**
     * Determines the server mode based on configuration settings.
     *
     * @return The detected server mode
     */
    private static ServerMode determineServerMode() {
        if (Bukkit.getOnlineMode()) {
            return ServerMode.ONLINE;
        }

        if (isProxy()) {
            return ServerMode.PROXY;
        }

        return ServerMode.OFFLINE;
    }

    /**
     * Checks if the server is running behind a proxy (BungeeCord or Velocity).
     *
     * @return {@code true} if a proxy is detected, {@code false} otherwise
     */
    private static boolean isProxy() {
        return isBungeeCordEnabled() || isVelocityEnabled();
    }

    /**
     * Checks if BungeeCord is enabled in the Spigot configuration.
     *
     * @return {@code true} if BungeeCord is enabled, {@code false} otherwise
     */
    private static boolean isBungeeCordEnabled() {
        return Bukkit.spigot().getConfig().getBoolean("settings.bungeecord", false);
    }

    /**
     * Checks if Velocity is enabled in the Paper global configuration.
     *
     * @return {@code true} if Velocity is enabled, {@code false} otherwise
     */
    private static boolean isVelocityEnabled() {
        File oldPaperConfig = new File(getRootFolder(), "paper.yml");
        if (oldPaperConfig.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(oldPaperConfig);
            return configuration.getBoolean("settings.velocity-support.enabled", false);
        }

        File paperGlobalConfig = new File(getRootFolder(), "config" + File.separator + "paper-global.yml");
        if (paperGlobalConfig.exists()) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(paperGlobalConfig);
            return configuration.getBoolean("proxies.velocity.enabled", false);
        }

        return false;
    }

    /**
     * Retrieves the server's root folder.
     *
     * @return A File object pointing server's root folder
     */
    private static File getRootFolder() {
        return JavaPlugin.getPlugin(BuildSystem.class).getDataFolder().getParentFile().getParentFile();
    }

    /**
     * Retrieves the current server mode.
     *
     * @return The detected server mode
     */
    public static ServerMode getServerMode() {
        return SERVER_MODE;
    }

    /**
     * Enum representing the possible server modes.
     */
    public enum ServerMode {

        /**
         * The server is online.
         */
        ONLINE,

        /**
         * The server is running behind a proxy.
         */
        PROXY,

        /**
         * The server is offline.
         */
        OFFLINE
    }
}
