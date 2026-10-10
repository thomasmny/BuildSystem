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
package de.eintosti.buildsystem.listener.world;

import de.eintosti.buildsystem.api.event.world.BuildWorldManipulationEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.event.EventDispatcher;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
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
import org.jspecify.annotations.NullMarked;

@NullMarked
public class WorldManipulateListener implements Listener {

    private final WorldStorageImpl worldStorage;
    private final ConfigService configService;
    private final WorldStatusRegistry worldStatusRegistry;
    private final SettingsService settingsService;
    private final EventDispatcher dispatcher;

    public WorldManipulateListener(
            WorldStorageImpl worldStorage,
            ConfigService configService,
            WorldStatusRegistry worldStatusRegistry,
            SettingsService settingsService) {
        this.worldStorage = worldStorage;
        this.configService = configService;
        this.worldStatusRegistry = worldStatusRegistry;
        this.settingsService = settingsService;
        this.dispatcher = new EventDispatcher(worldStorage);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        dispatcher.tryDispatchManipulationEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        dispatcher.tryDispatchManipulationEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof ArmorStand)) {
            return;
        }

        dispatcher.tryDispatchManipulationEvent(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        dispatcher.tryDispatchManipulationEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack != null
                && itemStack.getType()
                        == configService.current().settings().builder().worldEditWand()) {
            return;
        }

        Player player = event.getPlayer();
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return;
        }

        dispatcher.tryDispatchManipulationEvent(player, event);

        if (!buildWorld.getData().get(WorldDataKey.PHYSICS) && event.getClickedBlock() != null) {
            if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.FARMLAND) {
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

        WorldSetting setting = worldSettingFor(parentEvent);
        if (!buildWorld.getPermissions().canModify(player, setting)) {
            parentEvent.setCancelled(true);
            return;
        }

        worldData.set(WorldDataKey.LAST_EDITED, System.currentTimeMillis());
        updateStatus(worldData, player);
    }

    private WorldSetting worldSettingFor(Cancellable event) {
        return switch (event) {
            case BlockBreakEvent ignored -> WorldSetting.BLOCK_BREAKING;
            case BlockPlaceEvent ignored -> WorldSetting.BLOCK_PLACEMENT;
            default -> WorldSetting.BLOCK_INTERACTIONS;
        };
    }

    private void updateStatus(WorldData worldData, Player player) {
        // The status shown is a fallback for one that no longer exists; moving it on would overwrite the stored one.
        if (WorldDataImpl.isUnresolved(worldData, WorldDataKey.STATUS)) {
            return;
        }
        worldData
                .get(WorldDataKey.STATUS)
                .getProgressesTo()
                .flatMap(worldStatusRegistry::get)
                .ifPresent(next -> {
                    worldData.set(WorldDataKey.STATUS, next);
                    settingsService.forceUpdateSidebar(player);
                });
    }
}
