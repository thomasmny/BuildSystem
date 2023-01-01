/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import com.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.navigator.world.FilteredWorldsInventory.Visibility;
import com.eintosti.buildsystem.player.PlayerManager;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.BuildWorld;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author einTosti
 */
public class EditInventory implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final InventoryUtil inventoryUtil;
    private final PlayerManager playerManager;

    public EditInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        this.inventoryUtil = plugin.getInventoryUtil();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public Inventory getInventory(Player player, BuildWorld buildWorld) {
        Inventory inventory = Bukkit.createInventory(null, 54, Messages.getString("worldeditor_title"));
        fillGuiWithGlass(player, inventory);

        addBuildWorldInfoItem(inventory, buildWorld);

        addSettingsItem(inventory, 20, XMaterial.OAK_PLANKS, buildWorld.isBlockBreaking(), Messages.getString("worldeditor_blockbreaking_item"), Messages.getStringList("worldeditor_blockbreaking_lore"));
        addSettingsItem(inventory, 21, XMaterial.POLISHED_ANDESITE, buildWorld.isBlockPlacement(), Messages.getString("worldeditor_blockplacement_item"), Messages.getStringList("worldeditor_blockplacement_lore"));
        addSettingsItem(inventory, 22, XMaterial.SAND, buildWorld.isPhysics(), Messages.getString("worldeditor_physics_item"), Messages.getStringList("worldeditor_physics_lore"));
        addTimeItem(inventory, buildWorld);
        addSettingsItem(inventory, 24, XMaterial.TNT, buildWorld.isExplosions(), Messages.getString("worldeditor_explosions_item"), Messages.getStringList("worldeditor_explosions_lore"));
        inventoryUtil.addItemStack(inventory, 29, XMaterial.DIAMOND_SWORD, Messages.getString("worldeditor_butcher_item"), Messages.getStringList("worldeditor_butcher_lore"));
        addBuildersItem(inventory, buildWorld, player);
        addSettingsItem(inventory, 31, XMaterial.ARMOR_STAND, buildWorld.isMobAI(), Messages.getString("worldeditor_mobai_item"), Messages.getStringList("worldeditor_mobai_lore"));
        addVisibilityItem(inventory, buildWorld, player);
        addSettingsItem(inventory, 33, XMaterial.TRIPWIRE_HOOK, buildWorld.isBlockInteractions(), Messages.getString("worldeditor_blockinteractions_item"), Messages.getStringList("worldeditor_blockinteractions_lore"));
        inventoryUtil.addItemStack(inventory, 38, XMaterial.FILLED_MAP, Messages.getString("worldeditor_gamerules_item"), Messages.getStringList("worldeditor_gamerules_lore"));
        addDifficultyItem(inventory, buildWorld);
        inventoryUtil.addItemStack(inventory, 40, inventoryUtil.getStatusItem(buildWorld.getStatus()), Messages.getString("worldeditor_status_item"), getStatusLore(buildWorld));
        inventoryUtil.addItemStack(inventory, 41, XMaterial.ANVIL, Messages.getString("worldeditor_project_item"), getProjectLore(buildWorld));
        inventoryUtil.addItemStack(inventory, 42, XMaterial.PAPER, Messages.getString("worldeditor_permission_item"), getPermissionLore(buildWorld));

        return inventory;
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        player.openInventory(getInventory(player, buildWorld));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addBuildWorldInfoItem(Inventory inventory, BuildWorld buildWorld) {
        String displayName = Messages.getString("worldeditor_world_item", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));

        if (buildWorld.getMaterial() == XMaterial.PLAYER_HEAD) {
            inventoryUtil.addSkull(inventory, 4, displayName, buildWorld.getName());
        } else {
            inventoryUtil.addItemStack(inventory, 4, buildWorld.getMaterial(), displayName);
        }
    }

    private void addSettingsItem(Inventory inventory, int position, XMaterial material, boolean isEnabled, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemMeta.setLore(lore);
            itemMeta.addItemFlags(ItemFlag.values());
        }

        itemStack.setItemMeta(itemMeta);
        if (isEnabled) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    private void addTimeItem(Inventory inventory, BuildWorld buildWorld) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());

        XMaterial xMaterial = XMaterial.WHITE_STAINED_GLASS;
        String value = Messages.getString("worldeditor_time_lore_unknown");

        switch (getWorldTime(bukkitWorld)) {
            case SUNRISE:
                xMaterial = XMaterial.ORANGE_STAINED_GLASS;
                value = Messages.getString("worldeditor_time_lore_sunrise");
                break;
            case NOON:
                xMaterial = XMaterial.YELLOW_STAINED_GLASS;
                value = Messages.getString("worldeditor_time_lore_noon");
                break;
            case NIGHT:
                xMaterial = XMaterial.BLUE_STAINED_GLASS;
                value = Messages.getString("worldeditor_time_lore_night");
                break;
        }

        inventoryUtil.addItemStack(inventory, 23, xMaterial, Messages.getString("worldeditor_time_item"),
                Messages.getStringList("worldeditor_time_lore", new AbstractMap.SimpleEntry<>("%time%", value))
        );
    }

    public BuildWorld.Time getWorldTime(World bukkitWorld) {
        if (bukkitWorld == null) {
            return BuildWorld.Time.UNKNOWN;
        }

        int worldTime = (int) bukkitWorld.getTime();
        int noonTime = plugin.getConfigValues().getNoonTime();

        if (worldTime >= 0 && worldTime < noonTime) {
            return BuildWorld.Time.SUNRISE;
        } else if (worldTime >= noonTime && worldTime < 13000) {
            return BuildWorld.Time.NOON;
        } else {
            return BuildWorld.Time.NIGHT;
        }
    }

    private void addBuildersItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        UUID creatorId = buildWorld.getCreatorId();
        if ((creatorId != null && creatorId.equals(player.getUniqueId())) || player.hasPermission(BuildSystem.ADMIN_PERMISSION)) {
            addSettingsItem(inventory, 30, XMaterial.IRON_PICKAXE, buildWorld.isBuilders(), Messages.getString("worldeditor_builders_item"), Messages.getStringList("worldeditor_builders_lore"));
        } else {
            inventoryUtil.addItemStack(inventory, 30, XMaterial.BARRIER, Messages.getString("worldeditor_builders_not_creator_item"), Messages.getStringList("worldeditor_builders_not_creator_lore"));
        }
    }

    private void addVisibilityItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        int slot = 32;
        String displayName = Messages.getString("worldeditor_visibility_item");

        if (!playerManager.canCreateWorld(player, Visibility.matchVisibility(buildWorld.isPrivate()))) {
            inventoryUtil.addItemStack(inventory, slot, XMaterial.BARRIER, "§c§m" + ChatColor.stripColor(displayName));
            return;
        }

        XMaterial xMaterial = XMaterial.ENDER_EYE;
        List<String> lore = Messages.getStringList("worldeditor_visibility_lore_public");

        if (buildWorld.isPrivate()) {
            xMaterial = XMaterial.ENDER_PEARL;
            lore = Messages.getStringList("worldeditor_visibility_lore_private");
        }

        inventoryUtil.addItemStack(inventory, slot, xMaterial, displayName, lore);
    }

    private void addDifficultyItem(Inventory inventory, BuildWorld buildWorld) {
        XMaterial xMaterial;

        switch (buildWorld.getDifficulty()) {
            case EASY:
                xMaterial = XMaterial.GOLDEN_HELMET;
                break;
            case NORMAL:
                xMaterial = XMaterial.IRON_HELMET;
                break;
            case HARD:
                xMaterial = XMaterial.DIAMOND_HELMET;
                break;
            default:
                xMaterial = XMaterial.LEATHER_HELMET;
                break;
        }

        ArrayList<String> lore = new ArrayList<>();
        Messages.getStringList("worldeditor_difficulty_lore").forEach(line -> lore.add(line.replace("%difficulty%", buildWorld.getDifficultyName())));

        inventoryUtil.addItemStack(inventory, 39, xMaterial, Messages.getString("worldeditor_difficulty_item"), lore);
    }

    private List<String> getStatusLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String line : Messages.getStringList("worldeditor_status_lore")) {
            lore.add(line.replace("%status%", buildWorld.getStatus().getName()));
        }
        return lore;
    }

    private List<String> getProjectLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String line : Messages.getStringList("worldeditor_project_lore")) {
            lore.add(line.replace("%project%", buildWorld.getProject()));
        }
        return lore;
    }

    private List<String> getPermissionLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String line : Messages.getStringList("worldeditor_permission_lore")) {
            lore.add(line.replace("%permission%", buildWorld.getPermission()));
        }
        return lore;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtil.checkIfValidClick(event, "worldeditor_title")) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = plugin.getPlayerManager().getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_edit_error");
            return;
        }

        switch (event.getSlot()) {
            case 20:
                if (hasPermission(player, "buildsystem.edit.breaking")) {
                    buildWorld.setBlockBreaking(!buildWorld.isBlockBreaking());
                }
                break;
            case 21:
                if (hasPermission(player, "buildsystem.edit.placement")) {
                    buildWorld.setBlockPlacement(!buildWorld.isBlockPlacement());
                }
                break;
            case 22:
                if (hasPermission(player, "buildsystem.edit.physics")) {
                    buildWorld.setPhysics(!buildWorld.isPhysics());
                }
                break;
            case 23:
                if (hasPermission(player, "buildsystem.edit.time")) {
                    changeTime(player, buildWorld);
                }
                break;
            case 24:
                if (hasPermission(player, "buildsystem.edit.explosions")) {
                    buildWorld.setExplosions(!buildWorld.isExplosions());
                }
                break;

            case 29:
                if (hasPermission(player, "buildsystem.edit.entities")) {
                    removeEntities(player, buildWorld);
                }
                return;
            case 30:
                if (itemStack.getType() == XMaterial.BARRIER.parseMaterial()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (!hasPermission(player, "buildsystem.edit.builders")) {
                    return;
                }
                if (event.isRightClick()) {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    player.openInventory(plugin.getBuilderInventory().getInventory(buildWorld, player));
                    return;
                }
                buildWorld.setBuilders(!buildWorld.isBuilders());
                break;
            case 31:
                if (hasPermission(player, "buildsystem.edit.mobai")) {
                    buildWorld.setMobAI(!buildWorld.isMobAI());
                }
                break;
            case 32:
                if (itemStack.getType() == XMaterial.BARRIER.parseMaterial()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (!hasPermission(player, "buildsystem.edit.visibility")) {
                    return;
                }
                buildWorld.setPrivate(!buildWorld.isPrivate());
                break;
            case 33:
                if (hasPermission(player, "buildsystem.edit.interactions")) {
                    buildWorld.setBlockInteractions(!buildWorld.isBlockInteractions());
                }
                break;

            case 38:
                if (hasPermission(player, "buildsystem.edit.gamerules")) {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    plugin.getGameRules().resetInvIndex(player.getUniqueId());
                    plugin.getGameRuleInventory().openInventory(player, buildWorld);
                }
                return;
            case 39:
                if (hasPermission(player, "buildsystem.edit.difficulty")) {
                    buildWorld.cycleDifficulty();
                    buildWorld.getWorld().setDifficulty(buildWorld.getDifficulty());
                }
                break;
            case 40:
                if (hasPermission(player, "buildsystem.edit.status")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    plugin.getStatusInventory().openInventory(player);
                }
                return;
            case 41:
                if (hasPermission(player, "buildsystem.edit.project")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new SetProjectSubCommand(plugin, buildWorld.getName()).getProjectInput(player, buildWorld, false);
                }
                return;
            case 42:
                if (hasPermission(player, "buildsystem.edit.permission")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    new SetPermissionSubCommand(plugin, buildWorld.getName()).getPermissionInput(player, buildWorld, false);
                }
                return;

            default:
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        openInventory(player, buildWorld);
    }

    private boolean hasPermission(Player player, String permission) {
        if (player.hasPermission(permission)) {
            return true;
        }
        player.closeInventory();
        plugin.sendPermissionMessage(player);
        XSound.ENTITY_ITEM_BREAK.play(player);
        return false;
    }

    private void changeTime(Player player, BuildWorld buildWorld) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        BuildWorld.Time time = getWorldTime(bukkitWorld);
        switch (time) {
            case SUNRISE:
                bukkitWorld.setTime(configValues.getNoonTime());
                break;
            case NOON:
                bukkitWorld.setTime(configValues.getNightTime());
                break;
            case NIGHT:
                bukkitWorld.setTime(configValues.getSunriseTime());
                break;
        }

        openInventory(player, buildWorld);
    }

    private void removeEntities(Player player, BuildWorld buildWorld) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        AtomicInteger entitiesRemoved = new AtomicInteger();
        bukkitWorld.getEntities().stream()
                .filter(entity -> !IGNORED_ENTITIES.contains(entity.getType()))
                .forEach(entity -> {
                    entity.remove();
                    entitiesRemoved.incrementAndGet();
                });

        player.closeInventory();
        Messages.sendMessage(player, "worldeditor_butcher_removed", new AbstractMap.SimpleEntry<>("%amount%", entitiesRemoved.get()));
    }

    private static final Set<EntityType> IGNORED_ENTITIES = Sets.newHashSet(
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
}