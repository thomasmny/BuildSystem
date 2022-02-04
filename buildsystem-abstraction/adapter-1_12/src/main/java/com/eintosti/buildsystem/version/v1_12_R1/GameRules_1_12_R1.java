/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.version.v1_12_R1;

import com.eintosti.buildsystem.version.GameRules;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class GameRules_1_12_R1 implements GameRules {

    private final Map<UUID, Integer> invIndex;
    private final int[] slots = new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    private final String inventoryTitle;
    private final List<String> booleanEnabledLore, booleanDisabledLore;
    private final List<String> unknownLore;
    private final List<String> integerLore;

    private Inventory[] inventories;
    private int numGameRules = 0;

    public GameRules_1_12_R1(String inventoryTitle, List<String> booleanEnabledLore, List<String> booleanDisabledLore, List<String> unknownLore, List<String> integerLore) {
        this.invIndex = new HashMap<>();

        this.inventoryTitle = inventoryTitle;
        this.booleanEnabledLore = booleanEnabledLore;
        this.booleanDisabledLore = booleanDisabledLore;
        this.unknownLore = unknownLore;
        this.integerLore = integerLore;
    }

    private Inventory createInventory() {
        return Bukkit.createInventory(null, 45, inventoryTitle);
    }

    @Override
    public Inventory getInventory(Player player, World world) {
        addGameRules(world);
        return inventories[getInvIndex(player.getUniqueId())];
    }

    @Override
    public void addGameRules(World world) {
        String[] gameRules = world.getGameRules();

        int columnGameRule = 0, maxColumnGameRule = 14;
        setNumGameRules(world);
        int numInventories = (numGameRules % 15 == 0 ? numGameRules : numGameRules + 1) != 0 ? (numGameRules % 15 == 0 ? numGameRules : numGameRules + 1) : 1;

        inventories = new Inventory[numInventories];
        Inventory inventory = createInventory();

        int index = 0;
        inventories[index] = inventory;

        for (int i = 0; i < gameRules.length; i++) {
            addGameRuleItem(inventory, this.slots[columnGameRule++], world, world.getGameRules()[i]);
            if (columnGameRule > maxColumnGameRule) {
                columnGameRule = 0;
                inventory = createInventory();
                inventories[++index] = inventory;
            }
        }
    }

    private List<String> getLore(World world, String gameRule) {
        String gameRuleValue = world.getGameRuleValue(gameRule);
        List<String> lore;
        if (isBoolean(gameRuleValue)) {
            boolean enabled = Boolean.parseBoolean(gameRuleValue);
            if (enabled) {
                lore = this.booleanEnabledLore;
            } else {
                lore = this.booleanDisabledLore;
            }
        } else if (gameRule.equals("gameLoopFunction")) {
            List<String> unknownLore = new ArrayList<>();
            this.unknownLore.forEach(line -> unknownLore.add(line.replace("%value%", gameRuleValue)));
            lore = unknownLore;
        } else {
            List<String> integerLore = new ArrayList<>();
            this.integerLore.forEach(line -> integerLore.add(line.replace("%value%", gameRuleValue)));
            lore = integerLore;
        }
        return lore;
    }

    private void addGameRuleItem(Inventory inventory, int slot, World world, String gameRule) {
        ItemStack itemStack = new ItemStack(isEnabled(world, gameRule) ? Material.MAP : Material.EMPTY_MAP);

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("§e" + gameRule);
        itemMeta.setLore(getLore(world, gameRule));
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        inventory.setItem(slot, itemStack);
    }

    private boolean isEnabled(World world, String gameRule) {
        String gameRuleValue = world.getGameRuleValue(gameRule);
        if (isBoolean(gameRuleValue)) {
            return Boolean.parseBoolean(gameRuleValue);
        }
        return true;
    }

    private boolean isBoolean(String string) {
        return string.equalsIgnoreCase("true") || string.equalsIgnoreCase("false");
    }

    @Override
    public void toggleGameRule(InventoryClickEvent event, World world) {
        int slot = event.getSlot();
        if (!isValidSlot(slot)) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return;
        }

        if (!itemMeta.hasDisplayName()) {
            return;
        }
        String displayName = itemMeta.getDisplayName();

        String gameRule = ChatColor.stripColor(displayName);
        String gameRuleValue = world.getGameRuleValue(gameRule);
        if (isBoolean(gameRuleValue)) {
            boolean value = Boolean.parseBoolean(gameRuleValue);
            world.setGameRuleValue(gameRule, String.valueOf(!value));
        } else if (!gameRule.equals("gameLoopFunction")) {
            int value = Integer.parseInt(gameRuleValue);

            if (event.isShiftClick()) {
                if (event.isRightClick()) {
                    value += 10;
                } else if (event.isLeftClick()) {
                    value -= 10;
                }
            } else {
                if (event.isRightClick()) {
                    value += 1;
                } else if (event.isLeftClick()) {
                    value -= 1;
                }
            }

            world.setGameRuleValue(gameRule, String.valueOf(value));
        }
    }

    private boolean isValidSlot(int slot) {
        for (int i : this.slots) {
            if (i == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void incrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) + 1);
    }

    @Override
    public void decrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) - 1);
    }

    @Override
    public int getInvIndex(UUID uuid) {
        if (!invIndex.containsKey(uuid)) {
            invIndex.put(uuid, 0);
        }
        return invIndex.get(uuid);
    }

    @Override
    public int getNumGameRules() {
        return numGameRules;
    }

    private void setNumGameRules(World world) {
        String[] gameRules = world.getGameRules();
        int gameRuleLength = gameRules.length;
        this.numGameRules = (gameRuleLength / 15) + (gameRuleLength % 15 == 0 ? 0 : 1);
    }

    @Override
    public int[] getSlots() {
        return slots;
    }
}
