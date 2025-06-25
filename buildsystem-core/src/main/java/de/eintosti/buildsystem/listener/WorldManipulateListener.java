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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.event.EventDispatcher;
import de.eintosti.buildsystem.event.world.BuildWorldManipulationEvent;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WorldManipulateListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;
    private final EventDispatcher dispatcher;

    public WorldManipulateListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.dispatcher = new EventDispatcher(worldStorage);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof ArmorStand)) {
            return;
        }

        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack != null && itemStack.getType() == plugin.getConfigValues().getWorldEditWand().get()) {
            return;
        }

        Player player = event.getPlayer();
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return;
        }

        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(player, event);

        if (!buildWorld.getData().physics().get() && event.getClickedBlock() != null) {
            if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == XMaterial.FARMLAND.get()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldManipulation(BuildWorldManipulationEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        BuildWorld buildWorld = event.getBuildWorld();
        WorldData worldData = buildWorld.getData();

        Cancellable parentEvent = event.getParentEvent();
        boolean canModify = buildWorld.getPermissions().canModify(player, () -> getRelatedWorldSetting(parentEvent, worldData).get());
        if (!canModify) {
            parentEvent.setCancelled(true);
            denyPlayerInteraction(event);
            return;
        }

        worldData.lastEdited().set(System.currentTimeMillis());
        updateStatus(worldData, player);
    }

    private WorldData.Type<Boolean> getRelatedWorldSetting(Cancellable event, WorldData data) {
        if (event instanceof BlockBreakEvent) {
            return data.blockBreaking();
        }
        if (event instanceof BlockPlaceEvent) {
            return data.blockPlacement();
        }
        return data.blockInteractions();
    }

    private void denyPlayerInteraction(Event event) {
        if (event instanceof PlayerInteractEvent interactEvent) {
            interactEvent.setUseItemInHand(Event.Result.DENY);
            interactEvent.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    private void updateStatus(WorldData worldData, Player player) {
        if (worldData.status().get() == BuildWorldStatus.NOT_STARTED) {
            worldData.status().set(BuildWorldStatus.IN_PROGRESS);
            plugin.getPlayerService().forceUpdateSidebar(player);
        }
    }
}