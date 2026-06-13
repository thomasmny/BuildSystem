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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SlabListener implements Listener {

    private final SettingsService settingsManager;
    private final WorldStorageImpl worldStorage;
    private final WorldProtectionPolicy policy;

    public SlabListener(BuildSystemPlugin plugin) {
        this.settingsManager = plugin.getSettingsService();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.policy = new WorldProtectionPolicy();
    }

    @EventHandler
    public void manageSlabSetting(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!settingsManager.getSettings(player).isSlabBreaking()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Slab slab) || slab.getType() != Slab.Type.DOUBLE) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
        if (buildWorld != null
                && policy.mayModify(player, buildWorld, buildWorld.getData().blockPlacement()) != Denial.NONE) {
            return;
        }

        event.setCancelled(true);

        if (isTopHalf(player)) {
            slab.setType(Slab.Type.BOTTOM);
        } else {
            slab.setType(Slab.Type.TOP);
        }

        block.setBlockData(slab);
    }

    public boolean isTopHalf(Player player) {
        RayTraceResult result = player.rayTraceBlocks(6);
        if (result == null) {
            return false;
        }
        return Math.abs(result.getHitPosition().getY() % 1) < 0.5;
    }
}
