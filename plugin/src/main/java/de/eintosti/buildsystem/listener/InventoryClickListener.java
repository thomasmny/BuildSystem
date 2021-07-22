package de.eintosti.buildsystem.listener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.inventory.*;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.NoClipManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.navigator.NavigatorType;
import de.eintosti.buildsystem.object.settings.Colour;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.settings.WorldSort;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.object.world.WorldType;
import de.eintosti.buildsystem.util.external.UUIDFetcher;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import de.eintosti.buildsystem.version.GameRules;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * @author einTosti
 */
public class InventoryClickListener implements Listener {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final NoClipManager noClipManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    private final ArchiveInventory archiveInventory;
    private final BuilderInventory builderInventory;
    private final CreateInventory createInventory;
    private final EditInventory editInventory;
    private final GameRuleInventory gameRuleInventory;
    private final NavigatorInventory navigatorInventory;
    private final PrivateInventory privateInventory;
    private final SettingsInventory settingsInventory;
    private final WorldsInventory worldsInventory;

    private final GameRules gameRules;

    public InventoryClickListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.noClipManager = plugin.getNoClipManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        this.archiveInventory = plugin.getArchiveInventory();
        this.builderInventory = plugin.getBuilderInventory();
        this.createInventory = plugin.getCreateInventory();
        this.editInventory = plugin.getEditInventory();
        this.gameRuleInventory = plugin.getGameRuleInventory();
        this.navigatorInventory = plugin.getNavigatorInventory();
        this.privateInventory = plugin.getPrivateInventory();
        this.settingsInventory = plugin.getSettingsInventory();
        this.worldsInventory = plugin.getWorldsInventory();

        this.gameRules = plugin.getGameRules();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNavigatorInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("old_navigator_title"))) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        switch (event.getSlot()) {
            default:
                return;
            case 11:
                worldsInventory.openInventory(player);
                break;
            case 12:
                archiveInventory.openInventory(player);
                break;
            case 13:
                privateInventory.openInventory(player);
                break;
            case 15:
                settingsInventory.openInventory(player);
                break;
        }
        XSound.ENTITY_CHICKEN_EGG.play(player);
    }

    @EventHandler
    public void onWorldsInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("world_navigator_title"))) return;
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        Material itemType = itemStack.getType();
        if (itemType == XMaterial.PLAYER_HEAD.parseMaterial()) {
            switch (event.getSlot()) {
                case 45:
                    worldsInventory.decrementInv(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    worldsInventory.openInventory(player);
                    break;
                case 49:
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    createInventory.openInventory(player, CreateInventory.Page.PREDEFINED);
                    break;
                case 53:
                    worldsInventory.incrementInv(player);
                    worldsInventory.openInventory(player);
                    break;
            }
        }
        manageInventoryClick(event, player, itemStack);
    }

    @EventHandler
    public void onArchiveInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("archive_title"))) return;
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        Material itemType = itemStack.getType();
        if (itemType == XMaterial.PLAYER_HEAD.parseMaterial()) {
            switch (event.getSlot()) {
                case 45:
                    archiveInventory.decrementInv(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    archiveInventory.openInventory(player);
                    break;
                case 53:
                    archiveInventory.incrementInv(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    archiveInventory.openInventory(player);
                    break;
            }
        }
        manageInventoryClick(event, player, itemStack);
    }

    @EventHandler
    public void onPrivateInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("private_title"))) return;
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        Material itemType = itemStack.getType();
        if (itemType == XMaterial.PLAYER_HEAD.parseMaterial()) {
            switch (event.getSlot()) {
                case 45:
                    privateInventory.decrementInv(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    privateInventory.openInventory(player);
                    break;
                case 49:
                    player.closeInventory();
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    createInventory.openInventory(player, CreateInventory.Page.PREDEFINED);
                    worldManager.createPrivateWorldPlayers.add(player);
                    break;
                case 53:
                    privateInventory.incrementInv(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    privateInventory.openInventory(player);
                    break;
            }
        }
        manageInventoryClick(event, player, itemStack);
    }

    @EventHandler
    public void onSetupInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("setup_title"))) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        InventoryAction action = event.getAction();
        InventoryType type = event.getInventory().getType();
        int slot = event.getRawSlot();

        switch (action) {
            case PICKUP_ALL:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case PICKUP_HALF:
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
            case SWAP_WITH_CURSOR:
                if (type == InventoryType.CHEST) {
                    event.setCancelled(slot < 45 || slot > 80);
                    if (action == InventoryAction.SWAP_WITH_CURSOR) {
                        if (!(slot >= 45 && slot <= 80)) {
                            event.setCancelled(true);
                            if ((slot >= 11 && slot <= 15) || (slot >= 20 && slot <= 25) || (slot >= 29 && slot <= 34)) {
                                ItemStack itemStack = event.getCursor();
                                event.setCurrentItem(itemStack);
                                player.setItemOnCursor(null);
                            }
                        }
                    }
                }
                break;
            default:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onCreateInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("create_title"))) return;
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        boolean privateWorld = worldManager.createPrivateWorldPlayers.contains(player);

        if (event.getSlot() == 12) {
            createInventory.openInventory(player, CreateInventory.Page.PREDEFINED);
            if (privateWorld) worldManager.createPrivateWorldPlayers.add(player);
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return;
        } else if (event.getSlot() == 14) {
            createInventory.openInventory(player, CreateInventory.Page.TEMPLATES);
            if (privateWorld) {
                worldManager.createPrivateWorldPlayers.add(player);
            }
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null) return;

        CreateInventory.Page page = inventory.getItem(12).containsEnchantment(Enchantment.KNOCKBACK) ? CreateInventory.Page.PREDEFINED : CreateInventory.Page.TEMPLATES;
        if (page == CreateInventory.Page.PREDEFINED) {
            switch (event.getSlot()) {
                case 29:
                    worldManager.openWorldAnvil(player, WorldType.NORMAL, null, worldManager.createPrivateWorldPlayers.contains(player));
                    break;
                case 30:
                    worldManager.openWorldAnvil(player, WorldType.FLAT, null, worldManager.createPrivateWorldPlayers.contains(player));
                    break;
                case 31:
                    worldManager.openWorldAnvil(player, WorldType.NETHER, null, worldManager.createPrivateWorldPlayers.contains(player));
                    break;
                case 32:
                    worldManager.openWorldAnvil(player, WorldType.END, null, worldManager.createPrivateWorldPlayers.contains(player));
                    break;
                case 33:
                    worldManager.openWorldAnvil(player, WorldType.VOID, null, worldManager.createPrivateWorldPlayers.contains(player));
                    break;
            }
            XSound.ENTITY_CHICKEN_EGG.play(player);
        } else {
            if (itemStack.getType() == XMaterial.FILLED_MAP.parseMaterial()) {
                worldManager.openWorldAnvil(player, WorldType.TEMPLATE, itemStack.getItemMeta().getDisplayName(),
                        worldManager.createPrivateWorldPlayers.contains(player));
                XSound.ENTITY_CHICKEN_EGG.play(player);
            } else if (itemStack.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
                switch (event.getSlot()) {
                    case 38:
                        createInventory.decrementInv(player);
                        break;
                    case 42:
                        createInventory.incrementInv(player);
                        break;
                }
                XSound.ENTITY_CHICKEN_EGG.play(player);
                createInventory.openInventory(player, CreateInventory.Page.TEMPLATES);
            }
        }
    }

    @EventHandler
    public void onDeleteInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("delete_title"))) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.sendMessage(plugin.getString("worlds_delete_error"));
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 29:
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                player.sendMessage(plugin.getString("worlds_delete_canceled").replace("%world%", world.getName()));
                break;
            case 33:
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                worldManager.deleteWorld(player, world);
                break;
            default:
                return;
        }
        player.closeInventory();
    }

    @EventHandler
    public void onEditInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("worldeditor_title"))) {
            return;
        }
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_edit_error"));
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;

        switch (event.getSlot()) {
            case 20:
                world.setBlockBreaking(!world.isBlockBreaking());
                break;
            case 21:
                world.setBlockPlacement(!world.isBlockPlacement());
                break;
            case 22:
                world.setPhysics(!world.isPhysics());
                break;
            case 23:
                changeTime(player, world);
                break;
            case 24:
                world.setExplosions(!world.isExplosions());
                break;

            case 29:
                removeEntities(player, world);
                return;
            case 30:
                if (itemStack.getType() != XMaterial.BARRIER.parseMaterial()) {
                    if (event.isRightClick()) {
                        XSound.BLOCK_CHEST_OPEN.play(player);
                        player.openInventory(plugin.getBuilderInventory().getInventory(world, player));
                        return;
                    }
                    world.setBuilders(!world.isBuilders());
                }
                break;
            case 31:
                world.setMobAI(!world.isMobAI());
                break;
            case 32:
                world.setPrivate(!world.isPrivate());
                break;
            case 33:
                world.setBlockInteractions(!world.isBlockInteractions());
                break;

            case 38:
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getGameRuleInventory().openInventory(player, world);
                return;
            case 39:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                plugin.getStatusInventory().openInventory(player);
                return;
            case 41:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                plugin.getWorldsCommand().getProjectInput(player, false);
                return;
            case 42:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                plugin.getWorldsCommand().getPermissionInput(player, false);
                return;

            default:
                return;
        }
        XSound.ENTITY_CHICKEN_EGG.play(player);
        editInventory.openInventory(player, world);
    }

    private void changeTime(Player player, World world) {
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld == null) return;

        World.Time time = editInventory.getWorldTime(bukkitWorld);
        switch (time) {
            case SUNRISE:
                bukkitWorld.setTime(plugin.getNoonTime());
                break;
            case NOON:
                bukkitWorld.setTime(plugin.getNightTime());
                break;
            case NIGHT:
                bukkitWorld.setTime(plugin.getSunriseTime());
                break;
        }

        editInventory.openInventory(player, world);
    }

    private void removeEntities(Player player, World world) {
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld == null) return;

        int entitiesRemoved = 0;
        for (Entity entity : bukkitWorld.getEntities()) {
            if (isValid(entity)) {
                entity.remove();
                entitiesRemoved++;
            }
        }
        player.closeInventory();
        player.sendMessage(plugin.getString("worldeditor_butcher_removed").replace("%amount%", String.valueOf(entitiesRemoved)));
    }

    private boolean isValid(Entity entity) {
        return !IGNORED_ENTITIES.contains(entity.getType());
    }

    private static final ImmutableSet<EntityType> IGNORED_ENTITIES = Sets.immutableEnumSet(
            EntityType.ARMOR_STAND,
            EntityType.ENDER_CRYSTAL,
            EntityType.ITEM_FRAME,
            EntityType.FALLING_BLOCK,
            EntityType.MINECART,
            EntityType.MINECART_CHEST,
            EntityType.MINECART_COMMAND,
            EntityType.MINECART_FURNACE,
            EntityType.MINECART_HOPPER,
            EntityType.MINECART_MOB_SPAWNER,
            EntityType.MINECART_TNT,
            EntityType.PLAYER
    );

    @EventHandler
    public void onBuildersInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("worldeditor_builders_title"))) {
            return;
        }
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_addbuilder_error"));
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;

        Material material = itemStack.getType();
        if (material != XMaterial.PLAYER_HEAD.parseMaterial()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            editInventory.openInventory(player, world);
            return;
        }

        int slot = event.getSlot();
        switch (slot) {
            case 18:
                builderInventory.decrementInv(player);
                break;
            case 22:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                plugin.getWorldsCommand().getAddBuilderInput(player, false);
                return;
            case 26:
                builderInventory.incrementInv(player);
                break;
            default:
                if (slot == 4) return;
                if (!itemMeta.hasDisplayName()) return;
                if (!event.isShiftClick()) return;

                String builderName = ChatColor.stripColor(itemMeta.getDisplayName());
                UUID builderId = UUIDFetcher.getUUID(builderName);
                world.removeBuilder(builderId);
                XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
                player.sendMessage(plugin.getString("worlds_removebuilder_removed").replace("%builder%", builderName));
        }
        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.openInventory(builderInventory.getInventory(world, player));
    }

    @EventHandler
    public void onGameRuleInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("worldeditor_gamerules_title"))) {
            return;
        }
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_edit_error"));
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;
        Material material = itemStack.getType();

        if (material == XMaterial.PLAYER_HEAD.parseMaterial()) {
            int slot = event.getSlot();
            if (slot == 36) {
                gameRules.decrementInv(player);
            } else if (slot == 44) {
                gameRules.incrementInv(player);
            }
        } else if (material == XMaterial.FILLED_MAP.parseMaterial()
                || material == XMaterial.MAP.parseMaterial()) {
            org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
            gameRules.toggleGameRule(event, bukkitWorld);
        } else {
            XSound.BLOCK_CHEST_OPEN.play(player);
            editInventory.openInventory(player, world);
            return;
        }
        XSound.ENTITY_CHICKEN_EGG.play(player);
        gameRuleInventory.openInventory(player, world);
    }

    @EventHandler
    public void onStatusInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String selectedWorld = inventoryManager.selectedWorld(player);
        if (selectedWorld == null) return;

        if (!event.getView().getTitle().equals(plugin.getString("status_title").replace("%world%", selectedWorld))) {
            return;
        }
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;
        Material itemType = itemStack.getType();
        if (itemType == Material.AIR || !itemStack.hasItemMeta()) return;

        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setstatus_error"));
            return;
        }
        switch (event.getSlot()) {
            case 10:
                world.setStatus(WorldStatus.NOT_STARTED);
                break;
            case 11:
                world.setStatus(WorldStatus.IN_PROGRESS);
                break;
            case 12:
                world.setStatus(WorldStatus.ALMOST_FINISHED);
                break;
            case 13:
                world.setStatus(WorldStatus.FINISHED);
                break;
            case 14:
                world.setStatus(WorldStatus.ARCHIVE);
                break;
            case 16:
                world.setStatus(WorldStatus.HIDDEN);
                break;
            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                editInventory.openInventory(player, world);
                return;
        }
        plugin.forceUpdateSidebar(world);
        player.closeInventory();
        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.sendMessage(plugin.getString("worlds_setstatus_set").replace("%world%", world.getName()).replace("%status%", world.getStatusName()));
        plugin.selectedWorld.remove(player.getUniqueId());
    }

    @EventHandler
    public void onBlocksInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().equals(plugin.getString("blocks_title"))) return;
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        switch (event.getSlot()) {
            case 1:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_oak_barch"), "http://textures.minecraft.net/texture/22e4bb979efefd2ddb3f8b1545e59cd360492e12671ec371efc1f88af21ab83"));
                break;
            case 2:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_spruce_barch"), "http://textures.minecraft.net/texture/966cbdef8efb914d43a213be66b5396f75e5c1b9124f76f67d7cd32525748"));
                break;
            case 3:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_birch_barch"), "http://textures.minecraft.net/texture/a221f813dacee0fef8c59f76894dbb26415478d9ddfc44c2e708a6d3b7549b"));
                break;
            case 4:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_jungle_barch"), "http://textures.minecraft.net/texture/1cefc19380683015e47c666e5926b15ee57ab33192f6a7e429244cdffcc262"));
                break;
            case 5:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_acacia_barch"), "http://textures.minecraft.net/texture/96a3bba2b7a2b4fa46945b1471777abe4599695545229e782259aed41d6"));
                break;
            case 6:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_dark_oak_barch"), "http://textures.minecraft.net/texture/cde9d4e4c343afdb3ed68038450fc6a67cd208b2efc99fb622c718d24aac"));
                break;

            case 10:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_red_mushroom"), "http://textures.minecraft.net/texture/732dbd6612e9d3f42947b5ca8785bfb334258f3ceb83ad69a5cdeebea4cd65"));
                break;
            case 11:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_brown_mushroom"), "http://textures.minecraft.net/texture/fa49eca0369d1e158e539d78149acb1572949b88ba921d9ee694fea4c726b3"));
                break;
            case 12:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_full_mushroom_stem"), "http://textures.minecraft.net/texture/f55fa642d5ebcba2c5246fe6499b1c4f6803c10f14f5299c8e59819d5dc"));
                break;
            case 13:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_mushroom_stem"), "http://textures.minecraft.net/texture/84d541275c7f924bcb9eb2dbbf4b866b7649c330a6a013b53d584fd4ddf186ca"));
                break;
            case 14:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_mushroom_block"), "http://textures.minecraft.net/texture/3fa39ccf4788d9179a8795e6b72382d49297b39217146eda68ae78384355b13"));
                break;

            case 19:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_smooth_stone"), "http://textures.minecraft.net/texture/8dd0cd158c2bb6618650e3954b2d29237f5b4c0ddc7d258e17380ab6979f071"));
                break;
            case 20:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_double_stone_slab"), "http://textures.minecraft.net/texture/151e70169ea00f04a9439221cf33770844159dd775fc8830e311fd9b5ccd2969"));
                break;
            case 21:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_smooth_sandstone"), "http://textures.minecraft.net/texture/38fffbb0b8fdec6f62b17c451ab214fb86e4e355b116be961a9ae93eb49a43"));
                break;
            case 22:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_smooth_red_sandstone"), "http://textures.minecraft.net/texture/a2da7aa1ae6cc9d6c36c18a460d2398162edc2207fdfc9e28a7bf84d7441b8a2"));
                break;

            case 28:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_powered_redstone_lamp"), "http://textures.minecraft.net/texture/7eb4b34519fe15847dbea7229179feeb6ea57712d165dcc8ff6b785bb58911b0"));
                break;
            case 29:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_burning_furnace"), "http://textures.minecraft.net/texture/d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c"));
                break;
            case 30:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_command_block"), "http://textures.minecraft.net/texture/8514d225b262d847c7e557b474327dcef758c2c5882e41ee6d8c5e9cd3bc914"));
                break;
            case 31:
                playerInventory.addItem(inventoryManager.getItemStack(XMaterial.BARRIER, "§bBarrier"));
                break;

            case 37:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_mob_spawner"), "http://textures.minecraft.net/texture/db6bd9727abb55d5415265789d4f2984781a343c68dcaf57f554a5e9aa1cd"));
                break;
            case 38:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_nether_portal"), "http://textures.minecraft.net/texture/b0bfc2577f6e26c6c6f7365c2c4076bccee653124989382ce93bca4fc9e39b"));
                break;
            case 39:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_end_portal"), "http://textures.minecraft.net/texture/7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5"));
                break;
            case 40:
                playerInventory.addItem(inventoryManager.getUrlSkull(plugin.getString("blocks_dragon_egg"), "http://textures.minecraft.net/texture/3c151fb54b21fe5769ffb4825b5bc92da73657f214380e5d0301e45b6c13f7d"));
                break;
        }
    }

    @EventHandler
    public void onSettingsInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("settings_title"))) {
            return;
        }
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);

        switch (event.getSlot()) {
            case 11:
                settings.setSlabBreaking(!settings.isSlabBreaking());
                break;
            case 12:
                settings.setClearInventory(!settings.isClearInventory());
                break;
            case 13:
                plugin.getDesignInventory().openInventory(player);
                XSound.ENTITY_ITEM_PICKUP.play(player);
                return;
            case 14:
                if (!settings.isNoClip()) {
                    settings.setNoClip(true);
                    noClipManager.startNoClip(player);
                } else {
                    settings.setNoClip(false);
                    noClipManager.stopNoClip(player.getUniqueId());
                }
                break;
            case 15:
                settings.setInstantPlaceSigns(!settings.isInstantPlaceSigns());
                break;
            case 20:
                settings.setTrapDoor(!settings.isTrapDoor());
                break;
            case 21:
                if (!plugin.isScoreboard()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (settings.isScoreboard()) {
                    settings.setScoreboard(false);
                    settingsManager.stopScoreboard(player);
                } else {
                    settings.setScoreboard(true);
                    settingsManager.startScoreboard(player);
                    plugin.forceUpdateSidebar(player);
                }
                break;
            case 22:
                if (settings.getNavigatorType().equals(NavigatorType.OLD)) {
                    settings.setNavigatorType(NavigatorType.NEW);
                } else {
                    settings.setNavigatorType(NavigatorType.OLD);
                    plugin.getArmorStandManager().removeArmorStands(player);
                    if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                    }
                }
                break;
            case 23:
                settings.setSpawnTeleport(!settings.isSpawnTeleport());
                break;
            case 24:
                settings.setHidePlayers(!settings.isHidePlayers());
                toggleHidePlayers(player, settings);
                break;
            case 29:
                settings.setDisableInteract(!settings.isDisableInteract());
                break;
            case 30:
                switch (settings.getWorldSort()) {
                    case NAME_A_TO_Z:
                        settings.setWorldSort(WorldSort.NAME_Z_TO_A);
                        break;
                    case NAME_Z_TO_A:
                        settings.setWorldSort(WorldSort.PROJECT_A_TO_Z);
                        break;
                    case PROJECT_A_TO_Z:
                        settings.setWorldSort(WorldSort.PROJECT_Z_TO_A);
                        break;
                    case PROJECT_Z_TO_A:
                        settings.setWorldSort(WorldSort.NEWEST_FIRST);
                        break;
                    case NEWEST_FIRST:
                        settings.setWorldSort(WorldSort.OLDEST_FIRST);
                        break;
                    case OLDEST_FIRST:
                        settings.setWorldSort(WorldSort.NAME_A_TO_Z);
                        break;
                }
                break;
            case 31:
                if (!settings.isNightVision()) {
                    settings.setNightVision(true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                } else {
                    settings.setNightVision(false);
                    if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                }
                break;
            case 32:
                settings.setPlacePlants(!settings.isPlacePlants());
                break;
            default:
                return;
        }
        XSound.ENTITY_ITEM_PICKUP.play(player);
        plugin.getSettingsInventory().openInventory(player);
    }

    @SuppressWarnings("deprecation")
    private void toggleHidePlayers(Player player, Settings settings) {
        if (settings.isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        } else {
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }
    }

    @EventHandler
    public void onSpeedInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("speed_title"))) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("buildsystem.speed")) {
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 11:
                setSpeed(player, 0.2f, 1);
                break;
            case 12:
                setSpeed(player, 0.4f, 2);
                break;
            case 13:
                setSpeed(player, 0.6f, 3);
                break;
            case 14:
                setSpeed(player, 0.8f, 4);
                break;
            case 15:
                setSpeed(player, 1.0f, 5);
                break;
            default:
                return;
        }
        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.closeInventory();
    }

    private void setSpeed(Player player, float speed, int num) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            player.sendMessage(plugin.getString("speed_set_flying").replace("%speed%", String.valueOf(num)));
        } else {
            player.setWalkSpeed(speed);
            player.sendMessage(plugin.getString("speed_set_walking").replace("%speed%", String.valueOf(num)));
        }
    }

    @EventHandler
    public void onDesignInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getString("design_title"))) return;
        event.setCancelled(true);

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);

        if (itemStack.getType().toString().contains("STAINED_GLASS_PANE")) {
            plugin.getSettingsInventory().openInventory(player);
            return;
        }
        switch (event.getSlot()) {
            case 10:
                settings.setGlassColor(Colour.RED);
                break;
            case 11:
                settings.setGlassColor(Colour.ORANGE);
                break;
            case 12:
                settings.setGlassColor(Colour.YELLOW);
                break;
            case 13:
                settings.setGlassColor(Colour.PINK);
                break;
            case 14:
                settings.setGlassColor(Colour.MAGENTA);
                break;
            case 15:
                settings.setGlassColor(Colour.PURPLE);
                break;
            case 16:
                settings.setGlassColor(Colour.BROWN);
                break;
            case 18:
                settings.setGlassColor(Colour.LIME);
                break;
            case 19:
                settings.setGlassColor(Colour.GREEN);
                break;
            case 20:
                settings.setGlassColor(Colour.BLUE);
                break;
            case 21:
                settings.setGlassColor(Colour.CYAN);
                break;
            case 22:
                settings.setGlassColor(Colour.LIGHT_BLUE);
                break;
            case 23:
                settings.setGlassColor(Colour.WHITE);
                break;
            case 24:
                settings.setGlassColor(Colour.LIGHT_GREY);
                break;
            case 25:
                settings.setGlassColor(Colour.GREY);
                break;
            case 26:
                settings.setGlassColor(Colour.BLACK);
                break;
        }
        plugin.getDesignInventory().openInventory(player);
    }

    private void manageInventoryClick(InventoryClickEvent event, Player player, ItemStack itemStack) {
        if (itemStack == null) return;
        if (itemStack.getItemMeta() == null) return;
        ItemMeta itemMeta = itemStack.getItemMeta();
        int slot = event.getSlot();

        if (slot == 22) {
            if (itemMeta.hasDisplayName()) {
                if (itemMeta.getDisplayName().equals(plugin.getString("world_navigator_no_worlds"))
                        || itemMeta.getDisplayName().equals(plugin.getString("archive_no_worlds"))
                        || itemMeta.getDisplayName().equals(plugin.getString("private_no_worlds"))) {
                    return;
                }
            }
        }

        if (slot >= 9 && slot <= 44) {
            World world = worldManager.getWorld(getWorldName(itemMeta.getDisplayName()));
            manageWorldItemClick(event, player, itemMeta, world);
        }

        if (slot >= 45 && slot <= 53) {
            if (itemStack.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) {
                XSound.BLOCK_CHEST_OPEN.play(player);
                navigatorInventory.openInventory(player);
            }
        }
    }

    private void manageWorldItemClick(InventoryClickEvent event, Player player, ItemMeta itemMeta, World world) {
        if (event.isLeftClick()) {
            performNonEditClick(player, itemMeta);
        } else if (event.isRightClick()) {
            if (!player.hasPermission("buildsystem.edit")) {
                performNonEditClick(player, itemMeta);
                return;
            }

            if (world.isLoaded()) {
                plugin.selectedWorld.put(player.getUniqueId(), world);
                XSound.BLOCK_CHEST_OPEN.play(player);
                editInventory.openInventory(player, world);
            } else {
                player.closeInventory();
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                String subtitle = plugin.getString("world_not_loaded");
                Titles.sendTitle(player, "", subtitle);
            }
        }
    }

    private void performNonEditClick(Player player, ItemMeta itemMeta) {
        plugin.getPlayerMoveListener().closeNavigator(player);
        teleport(player, getWorldName(itemMeta.getDisplayName()));
    }

    private String getWorldName(String input) {
        String template = plugin.getString("world_item_title").replace("%world%", "");
        return StringUtils.difference(template, input);
    }

    private void teleport(Player player, String worldName) {
        World world = worldManager.getWorld(worldName);
        if (world == null) return;
        XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
        worldManager.teleport(player, world);
    }
}