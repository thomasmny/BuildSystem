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
package de.eintosti.buildsystem.util;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Utility class to determine the mode in which the server is running. The server can be in one of the following modes:
 *
 * <ul>
 *   <li>{@link ServerMode#ONLINE} - The server is running in online mode
 *   <li>{@link ServerMode#OFFLINE} - The server is running in offline mode
 * </ul>
 */
@NullMarked
public final class ServerModeChecker {

    private static volatile @Nullable ServerMode serverMode;

    private ServerModeChecker() {}

    /**
     * Retrieves the current server mode, working it out on first use.
     *
     * @return The detected server mode
     */
    public static ServerMode getServerMode() {
        ServerMode mode = serverMode;
        if (mode == null) {
            mode = isOnlineMode() ? ServerMode.ONLINE : ServerMode.OFFLINE;
            serverMode = mode;
        }
        return mode;
    }

    /**
     * A server behind an online-mode proxy runs in offline mode itself. Paper reports the effective mode through
     * {@code Server#getServerConfig().isProxyOnlineMode()}, which is reached by reflection because the plugin compiles
     * against Spigot. Spigot only knows its own setting.
     */
    private static boolean isOnlineMode() {
        try {
            Method getServerConfig = Server.class.getMethod("getServerConfig");
            Object serverConfig = getServerConfig.invoke(Bukkit.getServer());
            return (boolean) getServerConfig
                    .getReturnType()
                    .getMethod("isProxyOnlineMode")
                    .invoke(serverConfig);
        } catch (ReflectiveOperationException e) {
            return Bukkit.getOnlineMode();
        }
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
         * The server is offline.
         */
        OFFLINE
    }
}
