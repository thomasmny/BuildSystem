/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XTag;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.version.util.DirectionUtil;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.Builder;
import de.eintosti.buildsystem.world.WorldManager;
import de.eintosti.buildsystem.world.data.WorldData;
import de.eintosti.buildsystem.world.data.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SettingsInteractListener implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final PlayerManager playerManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    private final Set<UUID> cachePlayers;

    public SettingsInteractListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.playerManager = plugin.getPlayerManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        this.cachePlayers = new HashSet<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void manageIronDoorSetting(PlayerInteractEvent event) {
        if (!isValid(event)) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        boolean duelHanded = XMaterial.supports(9);
        if (duelHanded && event.getHand() != EquipmentSlot.valueOf("HAND")) {
            return;
        }

        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isTrapDoor()) {
            return;
        }

        Action action = event.getAction();
        XMaterial material = XMaterial.matchXMaterial(block.getType());
        if (action == Action.RIGHT_CLICK_BLOCK && (material == XMaterial.IRON_DOOR || material == XMaterial.IRON_TRAPDOOR)) {
            if (player.isSneaking()) {
                return;
            }

            event.setCancelled(true);
            switch (material) {
                case IRON_TRAPDOOR:
                    plugin.getCustomBlocks().toggleIronTrapdoor(event);
                    break;
                case IRON_DOOR:
                    plugin.getCustomBlocks().toggleIronDoor(event);
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void manageSlabSetting(PlayerInteractEvent event) {
        if (!isValid(event)) {
            return;
        }

        Player player = event.getPlayer();
        Action action = event.getAction();

        Settings settings = settingsManager.getSettings(player);
        if (settings.isSlabBreaking() && action == Action.LEFT_CLICK_BLOCK) {
            plugin.getCustomBlocks().modifySlab(event);
        }
    }

    @EventHandler
    public void managePlacePlantsSetting(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !isValid(event)) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack.getType());
        if (!XTag.FLOWERS.isTagged(xMaterial)
                && !XTag.REPLACEABLE_PLANTS.isTagged(xMaterial)
                && !XTag.ALIVE_CORAL_PLANTS.isTagged(xMaterial)
                && !XTag.DEAD_CORAL_PLANTS.isTagged(xMaterial)
                && !XTag.SAPLINGS.isTagged(xMaterial)) {
            return;
        }

        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isPlacePlants()) {
            return;
        }

        event.setCancelled(true);
        plugin.getCustomBlocks().setPlant(event);
    }

    @EventHandler
    public void manageInstantPlaceSignsSetting(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !isValid(event)) {
            return;
        }

        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isInstantPlaceSigns()) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (!XTag.SIGNS.isTagged(xMaterial)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        if (clickedBlock == null || blockFace == BlockFace.DOWN) {
            return;
        }

        Block adjacent = clickedBlock.getRelative(blockFace);
        if (adjacent.getType() != XMaterial.AIR.parseMaterial()) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        switch (blockFace) {
            case UP:
                if (!XMaterial.supports(13)) {
                    material = Material.getMaterial("SIGN_POST") != null ? Material.valueOf("SIGN_POST") : material;
                }
                adjacent.setType(material);
                plugin.getCustomBlocks().rotateBlock(adjacent, player, getDirection(player).getOppositeFace());
                break;
            case NORTH:
            case EAST:
            case SOUTH:
            case WEST:
                String type = xMaterial.name().replace("_SIGN", "");
                XMaterial.matchXMaterial(type + "_WALL_SIGN").ifPresent(value -> adjacent.setType(value.parseMaterial()));
                plugin.getCustomBlocks().rotateBlock(adjacent, player, blockFace);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void manageDisabledInteractSetting(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !isValid(event)) {
            return;
        }

        Settings settings = settingsManager.getSettings(player);
        if (!settings.isDisableInteract()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();
        XMaterial xMaterial = XMaterial.matchXMaterial(material);
        if (xMaterial == configValues.getWorldEditWand()) {
            return;
        }

        cachePlayers.add(player.getUniqueId());
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        if (!XMaterial.supports(13) && XTag.isItem(xMaterial)) {
            material = Material.valueOf(material.toString().replace("_ITEM", ""));
        }

        if (XTag.SIGNS.isTagged(xMaterial)) {
            if (!XMaterial.supports(13)) {
                material = Material.valueOf("WALL_SIGN");
            } else {
                String[] splitMaterial = material.toString().split("_");
                material = Material.valueOf(splitMaterial[0] + "_WALL_SIGN");
            }
        }

        if (!material.isBlock()) {
            return;
        }

        Block adjacent = block.getRelative(event.getBlockFace());
        adjacent.setType(material);
        XBlock.setColor(adjacent, getItemColor(itemStack));

        plugin.getCustomBlocks().rotateBlock(adjacent, player, DirectionUtil.getBlockDirection(player, false));

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(adjacent, adjacent.getState(), block, itemStack, player, true);
        Bukkit.getServer().getPluginManager().callEvent(blockPlaceEvent);
    }

    /**
     * Not every player can always interact with the {@link BuildWorld} they are in.
     * <p>
     * Reasons an interaction could be cancelled:<br>
     * - The world has its {@link WorldStatus} set to archived<br>
     * - The world has a setting enabled which disallows certain events<br>
     * - The world only allows {@link Builder}s to build and the player is not such a builder<br>
     * <p>
     * However, a player can override these reasons if:<br>
     * - The player has the permission `buildsystem.admin`<br>
     * - The player has the permission `buildsystem.bypass.archive`<br>
     * - The player has used `/build` to enter build-mode<br>
     *
     * @param event the event which was called by the world manipulation
     * @return if the interaction with the world is valid
     */
    private boolean isValid(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return false;
        }

        Player player = event.getPlayer();
        if (worldManager.canBypassBuildRestriction(player)) {
            return true;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            return true;
        }

        WorldData worldData = buildWorld.getData();
        boolean isInBuildMode = playerManager.isInBuildMode(player);
        if (worldData.status().get() == WorldStatus.ARCHIVE && !isInBuildMode) {
            return false;
        }

        if (!worldData.blockPlacement().get() && !isInBuildMode) {
            return false;
        }

        if (buildWorld.getData().buildersEnabled().get() && !buildWorld.isBuilder(player)) {
            return buildWorld.isCreator(player);
        }

        return true;
    }

    private BlockFace getDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        yaw %= 360;
        int i = (int) ((yaw + 8) / 22.5);
        switch (i) {
            case 1:
                return BlockFace.SOUTH_SOUTH_WEST;
            case 2:
                return BlockFace.SOUTH_WEST;
            case 3:
                return BlockFace.WEST_SOUTH_WEST;
            case 4:
                return BlockFace.WEST;
            case 5:
                return BlockFace.WEST_NORTH_WEST;
            case 6:
                return BlockFace.NORTH_WEST;
            case 7:
                return BlockFace.NORTH_NORTH_WEST;
            case 8:
                return BlockFace.NORTH;
            case 9:
                return BlockFace.NORTH_NORTH_EAST;
            case 10:
                return BlockFace.NORTH_EAST;
            case 11:
                return BlockFace.EAST_NORTH_EAST;
            case 12:
                return BlockFace.EAST;
            case 13:
                return BlockFace.EAST_SOUTH_EAST;
            case 14:
                return BlockFace.SOUTH_EAST;
            case 15:
                return BlockFace.SOUTH_SOUTH_EAST;
            default:
                return BlockFace.SOUTH;
        }
    }

    /**
     * Stop {@link Player} from opening {@link Inventory} because the event should be cancelled
     * as it was fired due to an interaction caused in {@link SettingsInteractListener#manageDisabledInteractSetting}
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (cachePlayers.remove(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    private DyeColor getItemColor(ItemStack itemStack) {
        return DyeColor.getByWoolData((byte) itemStack.getDurability());
    }
}