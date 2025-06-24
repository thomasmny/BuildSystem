/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

package de.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.util.PaginatedInventory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GameRulesInventory extends PaginatedInventory implements Listener {

    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    private final BuildSystemPlugin plugin;
    private int numGameRules = 0;

    public GameRulesInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());

        Inventory inventory = getInventory(player, bukkitWorld);
        fillGuiWithGlass(player, inventory);

        player.openInventory(inventory);
    }

    public Inventory getInventory(Player player, World world) {
        addGameRules(world, player);
        return inventories[getInvIndex(player)];
    }

    public void addGameRules(World world, Player player) {
        int columnGameRule = 0, maxColumnGameRule = 14;
        setNumGameRules(world);

        int numPages = numGameRules == 0 ? 1 : (int) Math.ceil((double) numGameRules / SLOTS.length);
        inventories = new Inventory[numPages];
        Inventory inventory = createInventory(player);

        int index = 0;
        inventories[index] = inventory;

        for (String gameRuleName : world.getGameRules()) {
            addGameRuleItem(inventory, SLOTS[columnGameRule++], world, gameRuleName, player);

            if (columnGameRule > maxColumnGameRule) {
                columnGameRule = 0;
                inventory = createInventory(player);
                inventories[++index] = inventory;
            }
        }
    }

    private void setNumGameRules(World world) {
        int numWorldGameRules = world.getGameRules().length;
        this.numGameRules = (numWorldGameRules / SLOTS.length) + (numWorldGameRules % SLOTS.length == 0 ? 0 : 1);
    }

    private Inventory createInventory(Player player) {
        return Bukkit.createInventory(null, 45, Messages.getString("worldeditor_gamerules_title", player));
    }

    private void addGameRuleItem(Inventory inventory, int slot, World world, String gameRuleName, Player player) {
        GameRule<?> gameRule = GameRule.getByName(gameRuleName);
        if (gameRule == null) {
            plugin.getLogger().severe("GameRule '" + gameRuleName + "' does not exist in world '" + world.getName() + "'.");
            return;
        }

        ItemStack itemStack = new ItemStack(isEnabled(world, gameRule) ? Material.FILLED_MAP : Material.MAP);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(ChatColor.YELLOW + gameRule.getName());
        itemMeta.setLore(getLore(world, gameRule, player));
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        inventory.setItem(slot, itemStack);
    }

    private List<String> getLore(World world, GameRule<?> gameRule, Player player) {
        List<String> lore;
        if (isBoolean(gameRule)) {
            lore = isEnabled(world, gameRule)
                    ? Messages.getStringList("worldeditor_gamerules_boolean_enabled", player)
                    : Messages.getStringList("worldeditor_gamerules_boolean_disabled", player);
        } else {
            lore = Messages.getStringList("worldeditor_gamerules_integer", player).stream()
                    .map(line -> line.replace("%value%", world.getGameRuleValue(gameRule).toString()))
                    .collect(Collectors.toList());
        }
        return lore;
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isValidSlot(i)) {
                InventoryUtils.addGlassPane(player, inventory, i);
            }
        }

        int invIndex = getInvIndex(player);

        if (numGameRules > 1 && invIndex > 0) {
            inventory.setItem(36, InventoryUtils.createSkull(Messages.getString("gui_previous_page", player), Profileable.detect("f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2")));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 36);
        }

        if (numGameRules > 1 && invIndex < (numGameRules - 1)) {
            inventory.setItem(44, InventoryUtils.createSkull(Messages.getString("gui_next_page", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158")));
        } else {
            InventoryUtils.addGlassPane(player, inventory, 44);
        }
    }

    public boolean isValidSlot(int slot) {
        return Arrays.stream(SLOTS).anyMatch(i -> i == slot);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!InventoryUtils.isValidClick(event, Messages.getString("worldeditor_gamerules_title", player))) {
            return;
        }

        BuildWorld buildWorld = plugin.getPlayerService().getPlayerStorage().getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_edit_error");
            return;
        }

        switch (XMaterial.matchXMaterial(event.getCurrentItem())) {
            case PLAYER_HEAD:
                int slot = event.getSlot();
                if (slot == 36) {
                    decrementInv(player, this.numGameRules, SLOTS.length);
                } else if (slot == 44) {
                    incrementInv(player, this.numGameRules, SLOTS.length);
                }
                openInventory(player, buildWorld);
                break;

            case FILLED_MAP:
            case MAP:
                modifyGameRule(event, buildWorld.getWorld());
                break;

            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getEditInventory().openInventory(player, buildWorld);
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        openInventory(player, buildWorld);
    }

    private void modifyGameRule(InventoryClickEvent event, World world) {
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

        String rawName = ChatColor.stripColor(itemMeta.getDisplayName());
        GameRule<?> gameRule = GameRule.getByName(rawName);

        GameRule<Boolean> booleanRule = asBooleanRule(gameRule);
        if (booleanRule != null) {
            boolean currentValue = Boolean.TRUE.equals(world.getGameRuleValue(booleanRule));
            world.setGameRule(booleanRule, !currentValue);
        } else {
            GameRule<Integer> integerRule = asIntegerRule(gameRule);
            int value = world.getGameRuleValue(integerRule);
            int delta = event.isShiftClick()
                    ? (event.isRightClick() ? 10 : event.isLeftClick() ? -10 : 0)
                    : (event.isRightClick() ? 1 : event.isLeftClick() ? -1 : 0);
            world.setGameRule(integerRule, value + delta);
        }
    }

    private static boolean isBoolean(GameRule<?> gameRule) {
        return gameRule != null && gameRule.getType().equals(Boolean.class);
    }

    @SuppressWarnings("unchecked")
    private static GameRule<Boolean> asBooleanRule(GameRule<?> gameRule) {
        return isBoolean(gameRule) ? (GameRule<Boolean>) gameRule : null;
    }

    private boolean isEnabled(World world, GameRule<?> rule) {
        GameRule<Boolean> booleanGameRule = asBooleanRule(rule);
        if (booleanGameRule != null) {
            return Boolean.TRUE.equals(world.getGameRuleValue(booleanGameRule));
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static GameRule<Integer> asIntegerRule(GameRule<?> rule) {
        return rule != null && rule.getType() == Integer.class ? (GameRule<Integer>) rule : null;
    }
}