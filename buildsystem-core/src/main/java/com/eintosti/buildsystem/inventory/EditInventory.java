/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.inventory.FilteredWorldsInventory.Visibility;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
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
    private final InventoryManager inventoryManager;
    private final PlayerManager playerManager;

    public EditInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        this.inventoryManager = plugin.getInventoryManager();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public Inventory getInventory(Player player, BuildWorld buildWorld) {
        Inventory inventory = Bukkit.createInventory(null, 54, plugin.getString("worldeditor_title"));
        fillGuiWithGlass(player, inventory);

        addBuildWorldInfoItem(inventory, buildWorld);

        addSettingsItem(inventory, 20, XMaterial.OAK_PLANKS, buildWorld.isBlockBreaking(), plugin.getString("worldeditor_blockbreaking_item"), plugin.getStringList("worldeditor_blockbreaking_lore"));
        addSettingsItem(inventory, 21, XMaterial.POLISHED_ANDESITE, buildWorld.isBlockPlacement(), plugin.getString("worldeditor_blockplacement_item"), plugin.getStringList("worldeditor_blockplacement_lore"));
        addSettingsItem(inventory, 22, XMaterial.SAND, buildWorld.isPhysics(), plugin.getString("worldeditor_physics_item"), plugin.getStringList("worldeditor_physics_lore"));
        addTimeItem(inventory, buildWorld);
        addSettingsItem(inventory, 24, XMaterial.TNT, buildWorld.isExplosions(), plugin.getString("worldeditor_explosions_item"), plugin.getStringList("worldeditor_explosions_lore"));
        inventoryManager.addItemStack(inventory, 29, XMaterial.DIAMOND_SWORD, plugin.getString("worldeditor_butcher_item"), plugin.getStringList("worldeditor_butcher_lore"));
        addBuildersItem(inventory, buildWorld, player);
        addSettingsItem(inventory, 31, XMaterial.ARMOR_STAND, buildWorld.isMobAI(), plugin.getString("worldeditor_mobai_item"), plugin.getStringList("worldeditor_mobai_lore"));
        addVisibilityItem(inventory, buildWorld, player);
        addSettingsItem(inventory, 33, XMaterial.TRIPWIRE_HOOK, buildWorld.isBlockInteractions(), plugin.getString("worldeditor_blockinteractions_item"), plugin.getStringList("worldeditor_blockinteractions_lore"));
        inventoryManager.addItemStack(inventory, 38, XMaterial.FILLED_MAP, plugin.getString("worldeditor_gamerules_item"), plugin.getStringList("worldeditor_gamerules_lore"));
        addDifficultyItem(inventory, buildWorld);
        inventoryManager.addItemStack(inventory, 40, inventoryManager.getStatusItem(buildWorld.getStatus()), plugin.getString("worldeditor_status_item"), getStatusLore(buildWorld));
        inventoryManager.addItemStack(inventory, 41, XMaterial.ANVIL, plugin.getString("worldeditor_project_item"), getProjectLore(buildWorld));
        inventoryManager.addItemStack(inventory, 42, XMaterial.PAPER, plugin.getString("worldeditor_permission_item"), getPermissionLore(buildWorld));

        return inventory;
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        player.openInventory(getInventory(player, buildWorld));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addBuildWorldInfoItem(Inventory inventory, BuildWorld buildWorld) {
        String displayName = plugin.getString("worldeditor_world_item").replace("%world%", buildWorld.getName());

        if (buildWorld.getMaterial() == XMaterial.PLAYER_HEAD) {
            inventoryManager.addSkull(inventory, 4, displayName, buildWorld.getName());
        } else {
            inventoryManager.addItemStack(inventory, 4, buildWorld.getMaterial(), displayName);
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
        String value = plugin.getString("worldeditor_time_lore_unknown");

        switch (getWorldTime(bukkitWorld)) {
            case SUNRISE:
                xMaterial = XMaterial.ORANGE_STAINED_GLASS;
                value = plugin.getString("worldeditor_time_lore_sunrise");
                break;
            case NOON:
                xMaterial = XMaterial.YELLOW_STAINED_GLASS;
                value = plugin.getString("worldeditor_time_lore_noon");
                break;
            case NIGHT:
                xMaterial = XMaterial.BLUE_STAINED_GLASS;
                value = plugin.getString("worldeditor_time_lore_night");
                break;
        }

        ArrayList<String> lore = new ArrayList<>();
        String finalValue = value;
        plugin.getStringList("worldeditor_time_lore").forEach(line -> lore.add(line.replace("%time%", finalValue)));

        inventoryManager.addItemStack(inventory, 23, xMaterial, plugin.getString("worldeditor_time_item"), lore);
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
            addSettingsItem(inventory, 30, XMaterial.IRON_PICKAXE, buildWorld.isBuilders(), plugin.getString("worldeditor_builders_item"), plugin.getStringList("worldeditor_builders_lore"));
        } else {
            inventoryManager.addItemStack(inventory, 30, XMaterial.BARRIER, plugin.getString("worldeditor_builders_not_creator_item"), plugin.getStringList("worldeditor_builders_not_creator_lore"));
        }
    }

    private void addVisibilityItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        int slot = 32;
        String displayName = plugin.getString("worldeditor_visibility_item");

        if (!playerManager.canCreateWorld(player, Visibility.matchVisibility(buildWorld.isPrivate()))) {
            inventoryManager.addItemStack(inventory, slot, XMaterial.BARRIER, "§c§m" + ChatColor.stripColor(displayName));
            return;
        }

        XMaterial xMaterial = XMaterial.ENDER_EYE;
        List<String> lore = plugin.getStringList("worldeditor_visibility_lore_public");

        if (buildWorld.isPrivate()) {
            xMaterial = XMaterial.ENDER_PEARL;
            lore = plugin.getStringList("worldeditor_visibility_lore_private");
        }

        inventoryManager.addItemStack(inventory, slot, xMaterial, displayName, lore);
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
        plugin.getStringList("worldeditor_difficulty_lore").forEach(line -> lore.add(line.replace("%difficulty%", buildWorld.getDifficultyName())));

        inventoryManager.addItemStack(inventory, 39, xMaterial, plugin.getString("worldeditor_difficulty_item"), lore);
    }

    private List<String> getStatusLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getStringList("worldeditor_status_lore")) {
            lore.add(line.replace("%status%", buildWorld.getStatus().getName()));
        }
        return lore;
    }

    private List<String> getProjectLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getStringList("worldeditor_project_lore")) {
            lore.add(line.replace("%project%", buildWorld.getProject()));
        }
        return lore;
    }

    private List<String> getPermissionLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getStringList("worldeditor_permission_lore")) {
            lore.add(line.replace("%permission%", buildWorld.getPermission()));
        }
        return lore;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "worldeditor_title")) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = plugin.getPlayerManager().getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_edit_error"));
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
                    plugin.getWorldsCommand().getProjectInput(player, false);
                }
                return;
            case 42:
                if (hasPermission(player, "buildsystem.edit.permission")) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    plugin.getWorldsCommand().getPermissionInput(player, false);
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
        player.sendMessage(plugin.getString("worldeditor_butcher_removed")
                .replace("%amount%", String.valueOf(entitiesRemoved.get()))
        );
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