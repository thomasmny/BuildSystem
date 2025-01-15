/*
 * Copyright (c) 2018-2024, Thomas Meaney
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

import java.util.UUID;
import org.bukkit.Bukkit;

@SuppressWarnings("deprecation")
public class UUIDFetcher {

    /**
     * Fetches the uuid which belongs to the player with the give name synchronously and returns it.
     *
     * @param name The name of the player whose uuid is to be fetched
     * @return The uuid which belongs to the player
     */
    public static UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    /**
     * Fetches the name which belongs to the player with the give uuid synchronously and returns it.
     *
     * @param uuid The uuid of the player whose name is to be fetched
     * @return The name which belongs to the player
     */
    public static String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}