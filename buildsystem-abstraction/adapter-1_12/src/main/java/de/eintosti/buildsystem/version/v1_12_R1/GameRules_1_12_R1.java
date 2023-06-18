/*
 * Copyright (c) 2018-2023, Thomas Meaney
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
package de.eintosti.buildsystem.version.v1_12_R1;

import de.eintosti.buildsystem.version.gamerules.AbstractGameRulesInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GameRules_1_12_R1 extends AbstractGameRulesInventory {

    private final List<String> booleanEnabledLore, booleanDisabledLore;
    private final List<String> unknownLore;
    private final List<String> integerLore;

    public GameRules_1_12_R1(String inventoryTitle, List<String> booleanEnabledLore, List<String> booleanDisabledLore, List<String> unknownLore, List<String> integerLore) {
        super(inventoryTitle);

        this.booleanEnabledLore = booleanEnabledLore;
        this.booleanDisabledLore = booleanDisabledLore;
        this.unknownLore = unknownLore;
        this.integerLore = integerLore;
    }

    @Override
    protected void addGameRuleItem(Inventory inventory, int slot, World world, String gameRule) {
        ItemStack itemStack = new ItemStack(isEnabled(world, gameRule) ? Material.MAP : Material.EMPTY_MAP);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(ChatColor.YELLOW + gameRule);
        itemMeta.setLore(getLore(world, gameRule));
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        inventory.setItem(slot, itemStack);
    }

    @Override
    protected boolean isEnabled(World world, String gameRule) {
        String gameRuleValue = world.getGameRuleValue(gameRule);
        if (isBoolean(gameRuleValue)) {
            return Boolean.parseBoolean(gameRuleValue);
        }
        return true;
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
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        String displayName = itemMeta.getDisplayName();
        String gameRule = ChatColor.stripColor(displayName);
        String gameRuleValue = world.getGameRuleValue(gameRule);

        if (isBoolean(gameRuleValue)) {
            boolean value = Boolean.parseBoolean(gameRuleValue);
            world.setGameRuleValue(gameRule, String.valueOf(!value));
        } else if (!gameRule.equalsIgnoreCase("gameLoopFunction")) {
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
}