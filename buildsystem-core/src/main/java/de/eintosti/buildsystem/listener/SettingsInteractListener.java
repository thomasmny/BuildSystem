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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XTag;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.DirectionUtil;
import de.eintosti.buildsystem.util.inventory.MaterialUtils;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Slab;
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
import org.bukkit.util.RayTraceResult;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SettingsInteractListener implements Listener {

    private static final EnumSet<XMaterial> OTHER_PLANTS = Sets.newEnumSet(Sets.newHashSet(
            XMaterial.TORCHFLOWER, XMaterial.PITCHER_PLANT, XMaterial.LILY_PAD, XMaterial.PINK_PETALS,
            XMaterial.BROWN_MUSHROOM, XMaterial.RED_MUSHROOM, XMaterial.CRIMSON_FUNGUS, XMaterial.WARPED_FUNGUS,
            XMaterial.SHORT_GRASS, XMaterial.FERN, XMaterial.DEAD_BUSH, XMaterial.LARGE_FERN, XMaterial.TALL_GRASS,
            XMaterial.NETHER_SPROUTS, XMaterial.WARPED_ROOTS, XMaterial.CRIMSON_ROOTS, XMaterial.SUGAR_CANE, XMaterial.BAMBOO,
            XMaterial.BIG_DRIPLEAF, XMaterial.SMALL_DRIPLEAF, XMaterial.SEAGRASS, XMaterial.SWEET_BERRIES
    ), XMaterial.class);

    private final CustomBlockManager customBlockManager;
    private final SettingsManager settingsManager;
    private final WorldStorageImpl worldStorage;

    private final Set<UUID> cachePlayers;

    public SettingsInteractListener(BuildSystemPlugin plugin) {
        this.customBlockManager = plugin.getCustomBlockManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldStorage = plugin.getWorldService().getWorldStorage();

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

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Settings settings = settingsManager.getSettings(player);
        if (!settings.isOpenTrapDoors()) {
            return;
        }

        XMaterial material = XMaterial.matchXMaterial(block.getType());
        if (material != XMaterial.IRON_DOOR && material != XMaterial.IRON_TRAPDOOR) {
            return;
        }

        event.setCancelled(true);
        Openable openable = (Openable) block.getBlockData();
        openable.setOpen(!openable.isOpen());
        block.setBlockData(openable);
    }

    @EventHandler
    public void manageSlabSetting(PlayerInteractEvent event) {
        if (!isValid(event)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isSlabBreaking()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Slab slab) || slab.getType() != Slab.Type.DOUBLE) {
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

    /**
     * Determines if the player's ray trace hit position is on the top half of a block.
     *
     * @param player The {@link Player} to check.
     * @return {@code true} if the player's hit position is on the top half of a block, {@code false} otherwise.
     */
    public boolean isTopHalf(Player player) {
        RayTraceResult result = player.rayTraceBlocks(6);
        if (result == null) {
            return false;
        }
        return Math.abs(result.getHitPosition().getY() % 1) < 0.5;
    }

    @EventHandler
    public void managePlacePlantsSetting(PlayerInteractEvent event) {
        if (!isValid(event)) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Settings settings = settingsManager.getSettings(event.getPlayer());
        if (!settings.isPlacePlants()) {
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
                && !OTHER_PLANTS.contains(xMaterial)
        ) {
            return;
        }

        event.setCancelled(true);
        Block adjacent = block.getRelative(event.getBlockFace());

        switch (xMaterial) {
            case SWEET_BERRIES:
                adjacent.setType(XMaterial.SWEET_BERRY_BUSH.get());
                break;
            case VINE:
                BlockFace toPlace = event.getBlockFace().getOppositeFace();
                if (toPlace == BlockFace.DOWN) { // Cannot place vines facing down
                    break;
                }
                adjacent.setType(xMaterial.get());
                MultipleFacing multipleFacing = (MultipleFacing) adjacent.getBlockData();
                Arrays.stream(DirectionUtil.BLOCK_SIDES).forEach(blockFace -> multipleFacing.setFace(blockFace, blockFace == toPlace));
                adjacent.setBlockData(multipleFacing);
                break;
            default:
                adjacent.setType(xMaterial.get());
                break;
        }
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
        if (!XTag.SIGNS.isTagged(xMaterial) && !XTag.HANGING_SIGNS.isTagged(xMaterial)) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        if (clickedBlock == null) {
            return;
        }

        Block adjacent = clickedBlock.getRelative(blockFace);
        if (adjacent.getType() != XMaterial.AIR.get()) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        boolean isHangingSign = XTag.HANGING_SIGNS.isTagged(xMaterial);

        switch (blockFace) {
            case UP -> {
                if (isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                customBlockManager.rotateBlock(adjacent, DirectionUtil.getPlayerDirection(player).getOppositeFace());
            }
            case DOWN -> {
                if (!isHangingSign) {
                    return;
                }
                adjacent.setType(material);
                customBlockManager.rotateBlock(adjacent, getHangingSignDirection(event));
            }
            case NORTH, EAST, SOUTH, WEST -> {
                String woodType = xMaterial.name()
                        .replace("_HANGING", "") // Replace hanging if present
                        .replace("_SIGN", ""); // Get wood type
                String block = isHangingSign ? "_WALL_HANGING_SIGN" : "_WALL_SIGN";
                BlockFace facing = isHangingSign ? getHangingSignDirection(event) : blockFace;
                XMaterial.matchXMaterial(woodType + block).ifPresent(value -> adjacent.setType(value.get()));
                customBlockManager.rotateBlock(adjacent, facing);
            }
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
        if (xMaterial == Config.Settings.Builder.worldEditWand) {
            return;
        }

        cachePlayers.add(player.getUniqueId());
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        if (XTag.SIGNS.isTagged(xMaterial) && event.getBlockFace() != BlockFace.UP) {
            String[] splitMaterial = material.toString().split("_");
            material = Material.valueOf(splitMaterial[0] + "_WALL_SIGN");
        }

        if (!material.isBlock()) {
            return;
        }

        Block adjacent = block.getRelative(event.getBlockFace());
        adjacent.setType(material);
        XBlock.setColor(adjacent, DyeColor.getByWoolData((byte) itemStack.getDurability()));

        customBlockManager.rotateBlock(adjacent, DirectionUtil.getBlockDirection(player, false));

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            hand = EquipmentSlot.HAND;
        }

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(adjacent, adjacent.getState(), block, itemStack, player, true, hand);
        Bukkit.getServer().getPluginManager().callEvent(blockPlaceEvent);
    }

    /**
     * Not every player can always interact with the {@link BuildWorld} they are in.
     * <p>
     * Reasons an interaction could be canceled:
     * <ul>
     *   <li>The world has its {@link BuildWorldStatus} set to archive</li>
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
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            return true;
        }

        if (buildWorld.getPermissions().canBypassBuildRestriction(player)) {
            return true;
        }

        WorldData worldData = buildWorld.getData();
        if (worldData.status().get() == BuildWorldStatus.ARCHIVE && !player.hasPermission("buildsystem.bypass.archive")) {
            return false;
        }

        if (!worldData.blockPlacement().get()) {
            return false;
        }

        Builders builders = buildWorld.getBuilders();
        if (buildWorld.getData().buildersEnabled().get()
                && !builders.isBuilder(player)
                && !player.hasPermission("buildsystem.bypass.builders")) {
            return builders.isCreator(player);
        }

        return true;
    }

    /**
     * Stop {@link Player} from opening {@link Inventory} because the event should be canceled as it was fired due to an interaction caused in
     * {@link SettingsInteractListener#manageDisabledInteractSetting}
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (cachePlayers.remove(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}