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
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
    private final int version = Integer.parseInt(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].replaceAll("[^0-9]", ""));
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
    public void onNavigatorPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.PHYSICAL) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) return;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        if (!itemMeta.hasDisplayName()) return;
        String displayName = itemMeta.getDisplayName();

        if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
            return;
        }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNonBuilderPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            return;
        }

        disableArchivedWorlds(buildWorld, player, event);
        checkWorldSettings(buildWorld, player, event);
        checkBuilders(buildWorld, player, event);

        if (buildWorld.isPhysics()) return;
        if (event.getClickedBlock() == null) return;

        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == XMaterial.FARMLAND.parseMaterial()) {
            event.setCancelled(true);
        }
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("buildsystem.admin")
                || player.hasPermission("buildsystem.bypass.archive")
                || plugin.buildPlayers.contains(player.getUniqueId());
    }

    private void disableArchivedWorlds(BuildWorld buildWorld, Player player, PlayerInteractEvent event) {
        if (!canBypass(player) && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    private void checkWorldSettings(BuildWorld buildWorld, Player player, PlayerInteractEvent event) {
        if (!canBypass(player) && buildWorld.isBlockInteractions()) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, PlayerInteractEvent event) {
        if (canBypass(player)) return;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onIronPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (isWithTwoHands()) {
            EquipmentSlot equipmentSlot = event.getHand();
            if (equipmentSlot != EquipmentSlot.valueOf("HAND")) {
                return;
            }
        }

        XMaterial material = XMaterial.matchXMaterial(block.getType());
        Settings settings = settingsManager.getSettings(player);

        if (!settings.isTrapDoor()) {
            return;
        }

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

    private boolean isWithTwoHands() {
        return this.version >= 192;
    }

    @EventHandler
    public void onSlabPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        Settings settings = settingsManager.getSettings(player);
        if (!settings.isSlabBreaking()) return;
        if (!action.equals(Action.LEFT_CLICK_BLOCK)) return;

        plugin.getCustomBlocks().modifySlab(event);
    }

    @EventHandler
    public void onPlacePlantsPlayerInteract(PlayerInteractEvent event) {
        if (!isValid(event)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        Material material = itemStack.getType();

        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);

        if (!settings.isPlacePlants()) return;
        if (clickedBlock.getType() == Material.FLOWER_POT) return;
        if (!PLANTS.contains(XMaterial.matchXMaterial(material))) return;

        event.setCancelled(true);
        plugin.getCustomBlocks().setPlant(event);
    }

    private boolean isValid(PlayerInteractEvent event) {
        Player player = event.getPlayer();
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
    public void onSignPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isInstantPlaceSigns()) return;

        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;
        Material material = itemStack.getType();
        XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);

        if (!SIGNS.contains(xMaterial)) return;

        Block clickedBlock = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        if (clickedBlock == null) return;

        Block adjacent = clickedBlock.getRelative(blockFace);
        if (adjacent.getType() != XMaterial.AIR.parseMaterial()) return;

        if (blockFace == BlockFace.DOWN) return;
        event.setUseItemInHand(Event.Result.DENY);

        switch (blockFace) {
            case UP:
                if (this.version < 1130) {
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
    public void onDisabledInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Settings settings = settingsManager.getSettings(player);
        if (!settings.isDisableInteract()) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        ItemStack itemStack = event.getItem();
        if (itemStack == null) return;

        Material material = itemStack.getType();
        if (material == XMaterial.WOODEN_AXE.parseMaterial()) return;

        if (DISABLED_BLOCKS.contains(XMaterial.matchXMaterial(block.getType()))) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);

            XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
            String materialName = material.toString();

            if (SIGNS.contains(xMaterial)) {
                if (this.version < 1130) {
                    material = Material.valueOf("WALL_SIGN");
                } else {
                    String[] splitMaterial = materialName.split("_");
                    material = Material.valueOf(splitMaterial[0] + "_WALL_SIGN");
                }
            }

            if (this.version < 1130) {
                if (materialName.endsWith("_ITEM")) {
                    material = Material.valueOf(materialName.replace("_ITEM", ""));
                }
            }

            Block adjacent = block.getRelative(event.getBlockFace());
            adjacent.setType(material);
            XBlock.setColor(adjacent, getItemColor(itemStack));

            plugin.getCustomBlocks().rotate(adjacent, player, null);
        }
    }

    @SuppressWarnings("deprecation")
    private DyeColor getItemColor(ItemStack itemStack) {
        return DyeColor.getByWoolData((byte) itemStack.getDurability());
    }

    private static final Set<XMaterial> DISABLED_BLOCKS = Sets.newHashSet(
            XMaterial.ACACIA_BUTTON,
            XMaterial.ACACIA_DOOR,
            XMaterial.ACACIA_FENCE,
            XMaterial.ACACIA_FENCE_GATE,
            XMaterial.ACACIA_SIGN,
            XMaterial.ACACIA_TRAPDOOR,
            XMaterial.ACACIA_WALL_SIGN,
            XMaterial.ANVIL,
            XMaterial.BARREL,
            XMaterial.BELL,
            XMaterial.BIRCH_BUTTON,
            XMaterial.BIRCH_DOOR,
            XMaterial.BIRCH_FENCE,
            XMaterial.BIRCH_FENCE_GATE,
            XMaterial.BIRCH_SIGN,
            XMaterial.BIRCH_TRAPDOOR,
            XMaterial.BIRCH_WALL_SIGN,
            XMaterial.BLACK_BED,
            XMaterial.BLACK_SHULKER_BOX,
            XMaterial.BLAST_FURNACE,
            XMaterial.BLUE_BED,
            XMaterial.BLUE_SHULKER_BOX,
            XMaterial.BROWN_BED,
            XMaterial.BROWN_SHULKER_BOX,
            XMaterial.CARTOGRAPHY_TABLE,
            XMaterial.CHEST,
            XMaterial.CHIPPED_ANVIL,
            XMaterial.CRAFTING_TABLE,
            XMaterial.CRIMSON_BUTTON,
            XMaterial.CRIMSON_DOOR,
            XMaterial.CRIMSON_FENCE,
            XMaterial.CRIMSON_FENCE_GATE,
            XMaterial.CRIMSON_SIGN,
            XMaterial.CRIMSON_TRAPDOOR,
            XMaterial.CRIMSON_WALL_SIGN,
            XMaterial.CYAN_BED,
            XMaterial.CYAN_SHULKER_BOX,
            XMaterial.DAMAGED_ANVIL,
            XMaterial.DARK_OAK_BUTTON,
            XMaterial.DARK_OAK_DOOR,
            XMaterial.DARK_OAK_FENCE,
            XMaterial.DARK_OAK_FENCE_GATE,
            XMaterial.DARK_OAK_SIGN,
            XMaterial.DARK_OAK_TRAPDOOR,
            XMaterial.DARK_OAK_WALL_SIGN,
            XMaterial.DAYLIGHT_DETECTOR,
            XMaterial.DISPENSER,
            XMaterial.DROPPER,
            XMaterial.ENCHANTING_TABLE,
            XMaterial.ENDER_CHEST,
            XMaterial.FURNACE,
            XMaterial.GRAY_BED,
            XMaterial.GRAY_SHULKER_BOX,
            XMaterial.GREEN_BED,
            XMaterial.GREEN_SHULKER_BOX,
            XMaterial.GRINDSTONE,
            XMaterial.HOPPER,
            XMaterial.IRON_DOOR,
            XMaterial.IRON_TRAPDOOR,
            XMaterial.JUKEBOX,
            XMaterial.JUNGLE_BUTTON,
            XMaterial.JUNGLE_DOOR,
            XMaterial.JUNGLE_FENCE,
            XMaterial.JUNGLE_FENCE_GATE,
            XMaterial.JUNGLE_SIGN,
            XMaterial.JUNGLE_TRAPDOOR,
            XMaterial.JUNGLE_WALL_SIGN,
            XMaterial.LEVER,
            XMaterial.LIGHT_BLUE_BED,
            XMaterial.LIGHT_BLUE_SHULKER_BOX,
            XMaterial.LIGHT_GRAY_BED,
            XMaterial.LIGHT_GRAY_SHULKER_BOX,
            XMaterial.LIME_BED,
            XMaterial.LOOM,
            XMaterial.MAGENTA_BED,
            XMaterial.MAGENTA_SHULKER_BOX,
            XMaterial.MOVING_PISTON,
            XMaterial.NETHER_BRICK_FENCE,
            XMaterial.NOTE_BLOCK,
            XMaterial.OAK_BUTTON,
            XMaterial.OAK_DOOR,
            XMaterial.OAK_FENCE,
            XMaterial.OAK_FENCE_GATE,
            XMaterial.OAK_SIGN,
            XMaterial.OAK_TRAPDOOR,
            XMaterial.OAK_WALL_SIGN,
            XMaterial.ORANGE_BED,
            XMaterial.ORANGE_SHULKER_BOX,
            XMaterial.PINK_BED,
            XMaterial.PINK_SHULKER_BOX,
            XMaterial.PISTON,
            XMaterial.PURPLE_BED,
            XMaterial.PURPLE_SHULKER_BOX,
            XMaterial.RED_BED,
            XMaterial.RED_SHULKER_BOX,
            XMaterial.SHULKER_BOX,
            XMaterial.SMITHING_TABLE,
            XMaterial.SMOKER,
            XMaterial.SPRUCE_BUTTON,
            XMaterial.SPRUCE_DOOR,
            XMaterial.SPRUCE_FENCE,
            XMaterial.SPRUCE_FENCE_GATE,
            XMaterial.SPRUCE_SIGN,
            XMaterial.SPRUCE_TRAPDOOR,
            XMaterial.SPRUCE_WALL_SIGN,
            XMaterial.STICKY_PISTON,
            XMaterial.STONE_BUTTON,
            XMaterial.STONECUTTER,
            XMaterial.TRAPPED_CHEST,
            XMaterial.WARPED_BUTTON,
            XMaterial.WARPED_DOOR,
            XMaterial.WARPED_FENCE,
            XMaterial.WARPED_FENCE_GATE,
            XMaterial.WARPED_SIGN,
            XMaterial.WARPED_TRAPDOOR,
            XMaterial.WARPED_WALL_SIGN,
            XMaterial.WHITE_BED,
            XMaterial.WHITE_SHULKER_BOX,
            XMaterial.YELLOW_BED,
            XMaterial.YELLOW_SHULKER_BOX
    );

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
