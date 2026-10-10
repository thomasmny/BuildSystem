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
package de.eintosti.buildsystem.listener.settings;

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Runs the per-player build settings that act on a block click. The first {@link SettingHandler} that takes the click
 * handles it, once the shared protection check has passed.
 */
@NullMarked
public class SettingInteractionListener implements Listener {

    private final SettingsService settingsService;
    private final WorldStorage worldStorage;
    private final WorldProtectionPolicy policy = new WorldProtectionPolicy();
    private final List<SettingHandler> handlers;

    public SettingInteractionListener(
            SettingsService settingsService, WorldStorage worldStorage, ConfigService configService) {
        this.settingsService = settingsService;
        this.worldStorage = worldStorage;
        this.handlers = List.of(
                new DisabledInteractionsHandler(configService),
                new InstantSignPlacementHandler(),
                new IronDoorHandler(),
                new PlantPlacementHandler(),
                new SlabHandler());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.isCancelled() || block == null) {
            return;
        }

        Player player = event.getPlayer();
        Settings settings = settingsService.getSettings(player);
        for (SettingHandler handler : handlers) {
            if (!handler.takes(event, block, settings)) {
                continue;
            }

            BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
            if (buildWorld == null
                    || policy.mayModify(player, buildWorld, WorldSetting.BLOCK_PLACEMENT) == Denial.NONE) {
                handler.handle(event, block);
            }
            return;
        }
    }

    /**
     * One setting's reaction to a block click.
     */
    interface SettingHandler {

        /**
         * {@return whether this setting reacts to the click} Checked before the protection check.
         */
        boolean takes(PlayerInteractEvent event, Block block, Settings settings);

        /**
         * Reacts to a click this handler {@link #takes takes}, after the protection check passed.
         */
        void handle(PlayerInteractEvent event, Block block);
    }
}
