/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.ArmorStandManager;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.Builder;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author einTosti
 */
public class PlayerInteractListener implements Listener {
    private final BuildSystem plugin;
    private final ArmorStandManager armorStandManager;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    public PlayerInteractListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.armorStandManager = plugin.getArmorStandManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void manageNavigatorInteraction(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
            return;
        }

        String displayName = itemMeta.getDisplayName();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        if (xMaterial == plugin.getNavigatorItem()) {
            if (!displayName.equals(plugin.getString("navigator_item"))) {
                return;
            }

            if (!player.hasPermission("buildsystem.gui")) {
                plugin.sendPermissionMessage(player);
                return;
            }

            event.setCancelled(true);
            openNavigator(player);
        } else if (xMaterial == XMaterial.BARRIER) {
            if (!displayName.equals(plugin.getString("barrier_item"))) {
                return;
            }

            event.setCancelled(true);
            plugin.getPlayerMoveListener().closeNavigator(player);
        }
    }

    private void openNavigator(Player player) {
        Settings settings = settingsManager.getSettings(player);

        switch (settings.getNavigatorType()) {
            case OLD:
                plugin.getNavigatorInventory().openInventory(player);
                XSound.BLOCK_CHEST_OPEN.play(player);
                break;
            case NEW:
                if (!plugin.openNavigator.contains(player)) {
                    summonNewNavigator(player);

                    String findItemName = plugin.getString("navigator_item");
                    ItemStack replaceItem = inventoryManager.getItemStack(XMaterial.BARRIER, plugin.getString("barrier_item"));

                    inventoryManager.replaceItem(player, findItemName, plugin.getNavigatorItem(), replaceItem);
                } else {
                    player.sendMessage(plugin.getString("worlds_navigator_open"));
                }
                break;
        }
    }

    private void summonNewNavigator(Player player) {
        UUID playerUuid = player.getUniqueId();
        plugin.playerWalkSpeed.put(playerUuid, player.getWalkSpeed());
        plugin.playerFlySpeed.put(playerUuid, player.getFlySpeed());

        player.setWalkSpeed(0);
        player.setFlySpeed(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 250, false, false));

        armorStandManager.spawnArmorStands(player);
        plugin.openNavigator.add(player);
    }

    @EventHandler
    public void onIronPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (isDuelHanded() && event.getHand() != EquipmentSlot.valueOf("HAND")) {
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
            }
        }
    }

    private boolean isDuelHanded() {
        return XMaterial.supports(9);
    }

    @EventHandler
    public void onSlabPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
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
    public void onPlacePlantsPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK || !isValid(event)) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();
        if (!PLANTS.contains(XMaterial.matchXMaterial(material))) {
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

    /**
     * Not every player can always interact with the {@link BuildWorld} they are in.
     * <p>
     * Reasons an interaction could be cancelled:
     * - The world has its {@link WorldStatus} set to archived
     * - The world has a setting enabled which disallows certain events
     * - The world only allows {@link Builder}s to build and the player is not such a builder
     * <p>
     * However, a player can override these reasons if:
     * - The player has the permission `buildsystem.admin`
     * - The player has the permission `buildsystem.bypass.archive`
     * - The player has used `/build` to enter build-mode
     *
     * @param event the event which was called by the world manipulation
     * @return if the interaction with the world is valid
     */
    private boolean isValid(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.canBypass(player)) {
            return true;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            return true;
        }

        boolean isInBuildMode = plugin.buildPlayers.contains(player.getUniqueId());
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !isInBuildMode) {
            return false;
        }

        if (!buildWorld.isBlockPlacement() && !isInBuildMode) {
            return false;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            return buildWorld.getCreatorId() == null || buildWorld.getCreatorId().equals(player.getUniqueId());
        }

        return true;
    }

    @EventHandler
    public void manageInstantPlaceSignsSetting(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
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
        if (!SIGNS.contains(xMaterial)) {
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

        if (blockFace == BlockFace.DOWN) {
            return;
        } else {
            event.setUseItemInHand(Event.Result.DENY);
        }

        switch (blockFace) {
            case UP:
                if (!XMaterial.supports(13)) {
                    material = Material.getMaterial("SIGN_POST") != null ? Material.valueOf("SIGN_POST") : material;
                }
                adjacent.setType(material);
                plugin.getCustomBlocks().rotate(adjacent, player, getDirection(player).getOppositeFace());
                break;
            case NORTH:
            case EAST:
            case SOUTH:
            case WEST:
                String[] splitName = xMaterial.name().split("_");
                Optional<XMaterial> parsedMaterial = XMaterial.matchXMaterial(splitName[0] + "_WALL_" + splitName[1]);
                parsedMaterial.ifPresent(value -> adjacent.setType(value.parseMaterial()));
                plugin.getCustomBlocks().rotate(adjacent, player, blockFace);
                break;
        }
    }

    private BlockFace getDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        yaw %= 360;
        int i = (int) ((yaw + 8) / 22.5);
        switch (i) {
            case 0:
                return BlockFace.SOUTH;
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
        }
        return BlockFace.SOUTH;
    }

    @EventHandler
    public void manageDisabledInteractSetting(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
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
        if (material == XMaterial.WOODEN_AXE.parseMaterial() || !isInteractable(block)) {
            return;
        }

        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
        String materialName = material.toString();

        if (SIGNS.contains(xMaterial)) {
            if (!XMaterial.supports(13)) {
                material = Material.valueOf("WALL_SIGN");
            } else {
                String[] splitMaterial = materialName.split("_");
                material = Material.valueOf(splitMaterial[0] + "_WALL_SIGN");
            }
        }

        if (!XMaterial.supports(13) && materialName.endsWith("_ITEM")) {
            material = Material.valueOf(materialName.replace("_ITEM", ""));
        }

        Block adjacent = block.getRelative(event.getBlockFace());
        adjacent.setType(material);
        XBlock.setColor(adjacent, getItemColor(itemStack));

        plugin.getCustomBlocks().rotate(adjacent, player, null);
    }

    private boolean isInteractable(Block block) {
        if (block == null) {
            return false;
        }

        Material type = block.getType();
        boolean interactable = type.isInteractable();
        if (!interactable) {
            return false;
        }

        // This check is done using string to support legacy MC versions too.
        String str = type.toString();
        return !(str.endsWith("_STAIRS") || str.endsWith("_FENCE")
                || str.equals("REDSTONE_ORE")
                || str.equals("REDSTONE_WIRE")
                || str.equals("PUMPKIN")
                || str.equals("MOVING_PISTON")
        );
    }

    @SuppressWarnings("deprecation")
    private DyeColor getItemColor(ItemStack itemStack) {
        return DyeColor.getByWoolData((byte) itemStack.getDurability());
    }

    private static final Set<XMaterial> SIGNS = Sets.newHashSet(
            XMaterial.ACACIA_SIGN,
            XMaterial.BIRCH_SIGN,
            XMaterial.CRIMSON_SIGN,
            XMaterial.DARK_OAK_SIGN,
            XMaterial.JUNGLE_SIGN,
            XMaterial.OAK_SIGN,
            XMaterial.SPRUCE_SIGN,
            XMaterial.WARPED_SIGN
    );

    private static final Set<XMaterial> PLANTS = Sets.newHashSet(
            XMaterial.ACACIA_SAPLING,
            XMaterial.ALLIUM,
            XMaterial.AZURE_BLUET,
            XMaterial.BEETROOT_SEEDS,
            XMaterial.BIRCH_SAPLING,
            XMaterial.BLUE_ORCHID,
            XMaterial.BROWN_MUSHROOM,
            XMaterial.CRIMSON_FUNGUS,
            XMaterial.CRIMSON_ROOTS,
            XMaterial.DANDELION,
            XMaterial.DARK_OAK_SAPLING,
            XMaterial.DEAD_BUSH,
            XMaterial.FERN,
            XMaterial.GRASS,
            XMaterial.JUNGLE_SAPLING,
            XMaterial.LARGE_FERN,
            XMaterial.LILAC,
            XMaterial.MELON_SEEDS,
            XMaterial.NETHER_SPROUTS,
            XMaterial.OAK_SAPLING,
            XMaterial.ORANGE_TULIP,
            XMaterial.OXEYE_DAISY,
            XMaterial.PEONY,
            XMaterial.PINK_TULIP,
            XMaterial.POPPY,
            XMaterial.PUMPKIN_SEEDS,
            XMaterial.RED_MUSHROOM,
            XMaterial.RED_TULIP,
            XMaterial.ROSE_BUSH,
            XMaterial.SEAGRASS,
            XMaterial.SPRUCE_SAPLING,
            XMaterial.SUNFLOWER,
            XMaterial.TALL_GRASS,
            XMaterial.TALL_SEAGRASS,
            XMaterial.TWISTING_VINES,
            XMaterial.WARPED_FUNGUS,
            XMaterial.WARPED_ROOTS,
            XMaterial.WEEPING_VINES,
            XMaterial.WHEAT_SEEDS,
            XMaterial.WHITE_TULIP
    );
}
