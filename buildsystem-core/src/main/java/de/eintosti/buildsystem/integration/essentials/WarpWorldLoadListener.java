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
package de.eintosti.buildsystem.integration.essentials;

import com.earth2me.essentials.utils.StringUtil;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.io.File;
import java.util.logging.Logger;
import net.ess3.api.events.UserWarpEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Loads the build world a warp points at before EssentialsX resolves it.
 *
 * <p>EssentialsX turns a warp into a {@link org.bukkit.Location} through {@link org.bukkit.Bukkit#getWorld}, which
 * returns {@code null} for a world BuildSystem has unloaded, failing the warp. {@link UserWarpEvent} fires before that
 * lookup, so loading the world here makes the warp resolve normally.
 *
 * <p>Only registered if EssentialsX is available.
 */
@NullMarked
public class WarpWorldLoadListener implements Listener {

    private final WorldStorageImpl worldStorage;
    private final File warpsFolder;

    public WarpWorldLoadListener(WorldStorageImpl worldStorage, Plugin essentials, Logger logger) {
        this.worldStorage = worldStorage;
        this.warpsFolder = new File(essentials.getDataFolder(), "warps");
        logger.info("EssentialsX warp world loading has been enabled.");
    }

    /**
     * Runs at the last priority before EssentialsX reads the event back, so the warp name is final and a warp another
     * plugin cancelled never loads a world.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUserWarp(UserWarpEvent event) {
        // World creation is main-thread only, so an off-thread warp is beyond saving here.
        if (event.isCancelled() || event.isAsynchronous()) {
            return;
        }

        String worldName = readWarpWorld(event.getWarp());
        if (worldName == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld == null || buildWorld.isLoaded()) {
            return;
        }

        // Blocking on purpose: EssentialsX resolves the location as soon as this returns.
        buildWorld.getLoader().load();
    }

    /**
     * Reads the world a warp points at straight off disk, because EssentialsX only exposes warps as already-resolved
     * locations - the very lookup that fails for an unloaded world. Warp files are named after the sanitized warp name,
     * so the target file is known without scanning the folder.
     *
     * @param warp The warp name
     * @return The world's name, or {@code null} if the warp is unknown or stores no usable name
     */
    private @Nullable String readWarpWorld(String warp) {
        File warpFile = new File(warpsFolder, StringUtil.sanitizeFileName(warp) + ".yml");
        if (!warpFile.isFile()) {
            return null;
        }

        // Since EssentialsX 2.19 "world" holds the world's uuid and "world-name" its name; older warps only have
        // "world", holding the name.
        YamlConfiguration warpConfig = YamlConfiguration.loadConfiguration(warpFile);
        String worldName = warpConfig.getString("world-name");
        return worldName != null && !worldName.isEmpty() ? worldName : warpConfig.getString("world");
    }
}
