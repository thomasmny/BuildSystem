package de.eintosti.buildsystem.listener;

import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.ArmorStandManager;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.util.external.xseries.XBlock;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import de.eintosti.buildsystem.util.external.xseries.XSound;
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

import static de.eintosti.buildsystem.util.external.xseries.XMaterial.*;

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
        } else if (xMaterial == BARRIER) {
            if (!displayName.equals(plugin.getString("barrier_item"))) {
                return;
            }

            event.setCancelled(true);
            plugin.getPlayerMoveListener().closeNavigator(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisablePlayerInteract(PlayerInteractEvent event) {
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

    private void disableArchivedWorlds(BuildWorld buildWorld, Player player, PlayerInteractEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive")) return;
        if (buildWorld.getStatus() == WorldStatus.ARCHIVE && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    private void checkWorldSettings(BuildWorld buildWorld, Player player, PlayerInteractEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.settings")) return;
        if (!buildWorld.isBlockInteractions() && !plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    private void checkBuilders(BuildWorld buildWorld, Player player, PlayerInteractEvent event) {
        if (player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.builders")) return;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
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

        if (action == Action.RIGHT_CLICK_BLOCK && (material == IRON_DOOR || material == IRON_TRAPDOOR)) {
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
                Optional<XMaterial> parsedMaterial = matchXMaterial(splitName[0] + "_WALL_" + splitName[1]);
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
            ACACIA_BUTTON,
            ACACIA_DOOR,
            ACACIA_FENCE,
            ACACIA_FENCE_GATE,
            ACACIA_SIGN,
            ACACIA_TRAPDOOR,
            ACACIA_WALL_SIGN,
            ANVIL,
            BARREL,
            BELL,
            BIRCH_BUTTON,
            BIRCH_DOOR,
            BIRCH_FENCE,
            BIRCH_FENCE_GATE,
            BIRCH_SIGN,
            BIRCH_TRAPDOOR,
            BIRCH_WALL_SIGN,
            BLACK_BED,
            BLACK_SHULKER_BOX,
            BLAST_FURNACE,
            BLUE_BED,
            BLUE_SHULKER_BOX,
            BROWN_BED,
            BROWN_SHULKER_BOX,
            CARTOGRAPHY_TABLE,
            CHEST,
            CHIPPED_ANVIL,
            CRAFTING_TABLE,
            CRIMSON_BUTTON,
            CRIMSON_DOOR,
            CRIMSON_FENCE,
            CRIMSON_FENCE_GATE,
            CRIMSON_SIGN,
            CRIMSON_TRAPDOOR,
            CRIMSON_WALL_SIGN,
            CYAN_BED,
            CYAN_SHULKER_BOX,
            DAMAGED_ANVIL,
            DARK_OAK_BUTTON,
            DARK_OAK_DOOR,
            DARK_OAK_FENCE,
            DARK_OAK_FENCE_GATE,
            DARK_OAK_SIGN,
            DARK_OAK_TRAPDOOR,
            DARK_OAK_WALL_SIGN,
            DAYLIGHT_DETECTOR,
            DISPENSER,
            DROPPER,
            ENCHANTING_TABLE,
            ENDER_CHEST,
            FURNACE,
            GRAY_BED,
            GRAY_SHULKER_BOX,
            GREEN_BED,
            GREEN_SHULKER_BOX,
            GRINDSTONE,
            HOPPER,
            IRON_DOOR,
            IRON_TRAPDOOR,
            JUKEBOX,
            JUNGLE_BUTTON,
            JUNGLE_DOOR,
            JUNGLE_FENCE,
            JUNGLE_FENCE_GATE,
            JUNGLE_SIGN,
            JUNGLE_TRAPDOOR,
            JUNGLE_WALL_SIGN,
            LEVER,
            LIGHT_BLUE_BED,
            LIGHT_BLUE_SHULKER_BOX,
            LIGHT_GRAY_BED,
            LIGHT_GRAY_SHULKER_BOX,
            LIME_BED,
            LOOM,
            MAGENTA_BED,
            MAGENTA_SHULKER_BOX,
            MOVING_PISTON,
            NETHER_BRICK_FENCE,
            NOTE_BLOCK,
            OAK_BUTTON,
            OAK_DOOR,
            OAK_FENCE,
            OAK_FENCE_GATE,
            OAK_SIGN,
            OAK_TRAPDOOR,
            OAK_WALL_SIGN,
            ORANGE_BED,
            ORANGE_SHULKER_BOX,
            PINK_BED,
            PINK_SHULKER_BOX,
            PISTON,
            PURPLE_BED,
            PURPLE_SHULKER_BOX,
            RED_BED,
            RED_SHULKER_BOX,
            SHULKER_BOX,
            SMITHING_TABLE,
            SMOKER,
            SPRUCE_BUTTON,
            SPRUCE_DOOR,
            SPRUCE_FENCE,
            SPRUCE_FENCE_GATE,
            SPRUCE_SIGN,
            SPRUCE_TRAPDOOR,
            SPRUCE_WALL_SIGN,
            STICKY_PISTON,
            STONE_BUTTON,
            STONECUTTER,
            TRAPPED_CHEST,
            WARPED_BUTTON,
            WARPED_DOOR,
            WARPED_FENCE,
            WARPED_FENCE_GATE,
            WARPED_SIGN,
            WARPED_TRAPDOOR,
            WARPED_WALL_SIGN,
            WHITE_BED,
            WHITE_SHULKER_BOX,
            YELLOW_BED,
            YELLOW_SHULKER_BOX
    );

    private static final Set<XMaterial> SIGNS = Sets.newHashSet(
            ACACIA_SIGN,
            BIRCH_SIGN,
            CRIMSON_SIGN,
            DARK_OAK_SIGN,
            JUNGLE_SIGN,
            OAK_SIGN,
            SPRUCE_SIGN,
            WARPED_SIGN
    );

    private static final Set<XMaterial> PLANTS = Sets.newHashSet(
            ACACIA_SAPLING,
            ALLIUM,
            AZURE_BLUET,
            BEETROOT_SEEDS,
            BIRCH_SAPLING,
            BLUE_ORCHID,
            BROWN_MUSHROOM,
            CRIMSON_FUNGUS,
            CRIMSON_ROOTS,
            DANDELION,
            DARK_OAK_SAPLING,
            DEAD_BUSH,
            FERN,
            GRASS,
            JUNGLE_SAPLING,
            LARGE_FERN,
            LILAC,
            MELON_SEEDS,
            NETHER_SPROUTS,
            OAK_SAPLING,
            ORANGE_TULIP,
            OXEYE_DAISY,
            PEONY,
            PINK_TULIP,
            POPPY,
            PUMPKIN_SEEDS,
            RED_MUSHROOM,
            RED_TULIP,
            ROSE_BUSH,
            SEAGRASS,
            SPRUCE_SAPLING,
            SUNFLOWER,
            TALL_GRASS,
            TALL_SEAGRASS,
            TWISTING_VINES,
            WARPED_FUNGUS,
            WARPED_ROOTS,
            WEEPING_VINES,
            WHEAT_SEEDS,
            WHITE_TULIP
    );
}
