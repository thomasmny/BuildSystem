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
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.util.MaterialUtils;
import de.eintosti.buildsystem.version.customblocks.CustomBlocks;
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SettingsInteractListener implements Listener {

    private final ConfigValues configValues;
    private final CustomBlocks customBlocks;

    private final PlayerManager playerManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    private final Set<UUID> cachePlayers;

    public SettingsInteractListener(BuildSystem plugin) {
        this.configValues = plugin.getConfigValues();
        this.customBlocks = plugin.getCustomBlocks();

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
                    customBlocks.toggleIronTrapdoor(event);
                    break;
                case IRON_DOOR:
                    customBlocks.toggleIronDoor(event);
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
            customBlocks.modifySlab(event);
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
                && !XTag.SAPLINGS.isTagged(xMaterial)
                && !OTHER_PLANTS.contains(xMaterial)) {
            return;
        }

        Settings settings = settingsManager.getSettings(event.getPlayer());
        if (!settings.isPlacePlants()) {
            return;
        }

        event.setCancelled(true);
        customBlocks.setPlant(event);
    }

    private static final EnumSet<XMaterial> OTHER_PLANTS = Sets.newEnumSet(Sets.newHashSet(
            XMaterial.TORCHFLOWER, XMaterial.PITCHER_PLANT, XMaterial.LILY_PAD, XMaterial.PINK_PETALS,
            XMaterial.BROWN_MUSHROOM, XMaterial.RED_MUSHROOM, XMaterial.CRIMSON_FUNGUS, XMaterial.WARPED_FUNGUS,
            XMaterial.GRASS, XMaterial.FERN, XMaterial.DEAD_BUSH, XMaterial.LARGE_FERN, XMaterial.TALL_GRASS,
            XMaterial.NETHER_SPROUTS, XMaterial.WARPED_ROOTS, XMaterial.CRIMSON_ROOTS, XMaterial.SUGAR_CANE, XMaterial.BAMBOO,
            XMaterial.BIG_DRIPLEAF, XMaterial.SMALL_DRIPLEAF, XMaterial.SEAGRASS, XMaterial.SWEET_BERRIES
    ), XMaterial.class);

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
        if (!XTag.SIGNS.isTagged(xMaterial) && !XTag.HANGING_SIGNS.isTagged(xMaterial)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        if (clickedBlock == null) {
            return;
        }

        Block adjacent = clickedBlock.getRelative(blockFace);
        if (adjacent.getType() != XMaterial.AIR.parseMaterial()) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        boolean isHangingSign = XTag.HANGING_SIGNS.isTagged(xMaterial);

        switch (blockFace) {
            case UP:
                if (isHangingSign) {
                    return;
                }
                if (!XMaterial.supports(13)) {
                    material = Material.getMaterial("SIGN_POST") != null ? Material.valueOf("SIGN_POST") : material;
                }
                adjacent.setType(material);
                customBlocks.rotateBlock(adjacent, player, DirectionUtil.getPlayerDirection(player).getOppositeFace());
                break;
            case DOWN:
                if (!isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                customBlocks.rotateBlock(adjacent, player, getHangingSignDirection(event));
                break;
            case NORTH:
            case EAST:
            case SOUTH:
            case WEST:
                String woodType = xMaterial.name()
                        .replace("_HANGING", "") // Replace hanging if present
                        .replace("_SIGN", ""); // Get wood type
                String block = isHangingSign ? "_WALL_HANGING_SIGN" : "_WALL_SIGN";
                BlockFace facing = isHangingSign ? getHangingSignDirection(event) : blockFace;
                XMaterial.matchXMaterial(woodType + block).ifPresent(value -> adjacent.setType(value.parseMaterial()));
                customBlocks.rotateBlock(adjacent, player, facing);
                break;
            default:
                break;
        }
    }

    private BlockFace getHangingSignDirection(PlayerInteractEvent event) {
        BlockFace clickedFace = event.getBlockFace();
        BlockFace playerFacing = DirectionUtil.getCardinalDirection(event.getPlayer()).getOppositeFace();
        if (clickedFace != playerFacing && clickedFace != playerFacing.getOppositeFace()) {
            return playerFacing;
        }
        return (clickedFace == BlockFace.NORTH || clickedFace == BlockFace.SOUTH) ? BlockFace.EAST : BlockFace.SOUTH;
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
        if (block == null || !MaterialUtils.isInteractable(XMaterial.matchXMaterial(block.getType()))) {
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

        if (XTag.SIGNS.isTagged(xMaterial) && event.getBlockFace() != BlockFace.UP) {
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
        XBlock.setColor(adjacent, DyeColor.getByWoolData((byte) itemStack.getDurability()));

        customBlocks.rotateBlock(adjacent, player, DirectionUtil.getBlockDirection(player, false));

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(adjacent, adjacent.getState(), block, itemStack, player, true);
        Bukkit.getServer().getPluginManager().callEvent(blockPlaceEvent);
    }

    /**
     * Not every player can always interact with the {@link BuildWorld} they are in.
     * <p>
     * Reasons an interaction could be cancelled:
     * <ul>
     *   <li>The world has its {@link WorldStatus} set to archived</li>
     *   <li>The world has a setting enabled which disallows certain events</li>
     *   <li>The world only allows {@link Builder}s to build and the player is not such a builder</li>
     * </ul>
     * <p>
     * However, a player can override these reasons if:
     * <ul>
     *   <li>The player has the permission <b>buildsystem.admin</b></li>
     *   <li>The player has the permission <b>buildsystem.bypass.archive</b></li>
     *   <li>The player has used <b>/build</b> to enter build-mode</li>
     * </ul>
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
}