/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.version;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * @author einTosti
 */
public class GameRules_1_13_R1 implements GameRules {
    private final Map<UUID, Integer> invIndex;
    private final int[] slots = new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    private final String inventoryTitle;
    private final List<String> booleanEnabledLore, booleanDisabledLore;
    private final List<String> integerLore;

    private Inventory[] inventories;
    private int numGameRules = 0;

    public GameRules_1_13_R1(String inventoryTitle, List<String> booleanEnabledLore, List<String> booleanDisabledLore, List<String> integerLore) {
        this.invIndex = new HashMap<>();

        this.inventoryTitle = inventoryTitle;
        this.booleanEnabledLore = booleanEnabledLore;
        this.booleanDisabledLore = booleanDisabledLore;
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

    private boolean isBoolean(String gameRuleName) {
        GameRule<?> gameRule = GameRule.getByName(gameRuleName);
        if (gameRule == null) return false;
        return gameRule.getType().equals(Boolean.class);
    }

    private void addGameRuleItem(Inventory inventory, int slot, World world, String gameRule) {
        ItemStack itemStack = new ItemStack(isEnabled(world, gameRule) ? Material.FILLED_MAP : Material.MAP);

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("§e" + gameRule);
        itemMeta.setLore(getLore(world, gameRule));
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        inventory.setItem(slot, itemStack);
    }

    private boolean isEnabled(World world, String gameRuleName) {
        GameRule<?> gameRule = GameRule.getByName(gameRuleName);
        if (gameRule == null) return false;
        if (gameRule.getType().equals(Boolean.class)) {
            return (Boolean) world.getGameRuleValue(gameRule);
        }
        return true;
    }

    private List<String> getLore(World world, String gameRule) {
        List<String> lore;
        if (isBoolean(gameRule)) {
            boolean enabled = (Boolean) world.getGameRuleValue(GameRule.getByName(gameRule));
            if (enabled) {
                lore = this.booleanEnabledLore;
            } else {
                lore = this.booleanDisabledLore;
            }
        } else {
            List<String> integerLore = new ArrayList<>();
            this.integerLore.forEach(line -> integerLore.add(
                    line.replace("%value%", "" + world.getGameRuleValue(GameRule.getByName(gameRule)))));
            lore = integerLore;
        }
        return lore;
    }

    private boolean isValidSlot(int slot) {
        for (int i : this.slots) {
            if (i == slot) return true;
        }
        return false;
    }

    @Override
    public void toggleGameRule(InventoryClickEvent event, World world) {
        int slot = event.getSlot();
        if (!isValidSlot(slot)) return;

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;

        if (!itemMeta.hasDisplayName()) return;
        String displayName = itemMeta.getDisplayName();

        String gameRuleName = ChatColor.stripColor(displayName);
        if (isBoolean(gameRuleName)) {
            GameRule<Boolean> gameRule = (GameRule<Boolean>) GameRule.getByName(gameRuleName);
            Boolean value = world.getGameRuleValue(gameRule);
            world.setGameRule(gameRule, !value);
        } else {
            GameRule<Integer> gameRule = (GameRule<Integer>) GameRule.getByName(gameRuleName);
            Integer value = world.getGameRuleValue(gameRule);

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

            world.setGameRule(gameRule, value);
        }
    }

    @Override
    public int getInvIndex(UUID uuid) {
        if (!invIndex.containsKey(uuid)) {
            invIndex.put(uuid, 0);
        }
        return invIndex.get(uuid);
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
