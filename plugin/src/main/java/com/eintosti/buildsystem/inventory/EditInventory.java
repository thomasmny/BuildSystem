/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class EditInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public EditInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
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
        addPrivateItem(inventory, buildWorld);
        addSettingsItem(inventory, 33, XMaterial.TRIPWIRE_HOOK, buildWorld.isBlockInteractions(), plugin.getString("worldeditor_blockinteractions_item"), plugin.getStringList("worldeditor_blockinteractions_lore"));
        inventoryManager.addItemStack(inventory, 38, XMaterial.FILLED_MAP, plugin.getString("worldeditor_gamerules_item"), plugin.getStringList("worldeditor_gamerules_lore"));
        inventoryManager.addItemStack(inventory, 39, inventoryManager.getStatusItem(buildWorld.getStatus()), plugin.getString("worldeditor_status_item"), getStatusLore(buildWorld));
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
        BuildWorld.Time time = getWorldTime(bukkitWorld);

        switch (time) {
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
        int noonTime = plugin.getNoonTime();
        if (worldTime >= 0 && worldTime < noonTime) {
            return BuildWorld.Time.SUNRISE;
        } else if (worldTime >= noonTime && worldTime < 13000) {
            return BuildWorld.Time.NOON;
        } else {
            return BuildWorld.Time.NIGHT;
        }
    }

    private void addBuildersItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        if ((buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId()))
                || player.hasPermission("buildsystem.admin")) {
            addSettingsItem(inventory, 30, XMaterial.IRON_PICKAXE, buildWorld.isBuilders(), plugin.getString("worldeditor_builders_item"), plugin.getStringList("worldeditor_builders_lore"));
        } else {
            inventoryManager.addItemStack(inventory, 30, XMaterial.BARRIER, plugin.getString("worldeditor_builders_not_creator_item"), plugin.getStringList("worldeditor_builders_not_creator_lore"));
        }
    }

    private void addPrivateItem(Inventory inventory, BuildWorld buildWorld) {
        XMaterial xMaterial = XMaterial.ENDER_EYE;
        List<String> lore = plugin.getStringList("worldeditor_visibility_lore_public");

        if (buildWorld.isPrivate()) {
            xMaterial = XMaterial.ENDER_PEARL;
            lore = plugin.getStringList("worldeditor_visibility_lore_private");
        }

        inventoryManager.addItemStack(inventory, 32, xMaterial, plugin.getString("worldeditor_visibility_item"), lore);
    }

    private List<String> getStatusLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String s : plugin.getStringList("worldeditor_status_lore")) {
            lore.add(s.replace("%status%", buildWorld.getStatusName()));
        }
        return lore;
    }

    private List<String> getProjectLore(BuildWorld buildWorld) {
        List<String> lore = new ArrayList<>();
        for (String s : plugin.getStringList("worldeditor_project_lore")) {
            lore.add(s.replace("%project%", buildWorld.getProject()));
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
}
