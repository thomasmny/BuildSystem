/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
import de.eintosti.buildsystem.util.inventory.BuildWorldHolder;
import de.eintosti.buildsystem.util.inventory.InventoryManager;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.util.inventory.PaginatedInventory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class GameRulesInventory extends PaginatedInventory {

    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    private final BuildSystemPlugin plugin;
    private final InventoryManager inventoryManager;

    private int numGameRules = 0;

    public GameRulesInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        Inventory inventory = getInventory(buildWorld, player);
        this.inventoryManager.registerInventoryHandler(inventory, this);
        player.openInventory(inventory);
    }

    public Inventory getInventory(BuildWorld buildWorld, Player player) {
        addGameRules(buildWorld, player);
        Inventory inventory = inventories[getInvIndex(player)];
        fillGuiWithGlass(player, inventory);
        return inventory;
    }

    public void addGameRules(BuildWorld buildWorld, Player player) {
        World world = buildWorld.getWorld();
        if (world == null) {
            player.closeInventory();
            plugin.getLogger().severe("World '" + buildWorld.getName() + "' does not exist.");
            return;
        }

        this.numGameRules = world.getGameRules().length;
        int numPages = calculateNumPages(this.numGameRules, SLOTS.length);
        inventories = new Inventory[numPages];
        Inventory inventory = createInventory(buildWorld, player);

        int index = 0;
        inventories[index] = inventory;

        int columnGameRule = 0, maxColumnGameRule = 14;
        for (String gameRuleName : world.getGameRules()) {
            addGameRuleItem(inventory, SLOTS[columnGameRule++], world, gameRuleName, player);

            if (columnGameRule > maxColumnGameRule) {
                columnGameRule = 0;
                inventory = createInventory(buildWorld, player);
                inventories[++index] = inventory;
            }
        }
    }

    @Contract("_, _ -> new")
    private Inventory createInventory(BuildWorld buildWorld, Player player) {
        return new GameRulesInventoryHolder(buildWorld, player).getInventory();
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
        if (isOfType(gameRule, Boolean.class)) {
            lore = isEnabled(world, gameRule)
                    ? Messages.getStringList("worldeditor_gamerules_boolean_enabled", player)
                    : Messages.getStringList("worldeditor_gamerules_boolean_disabled", player);
        } else {
            lore = Messages.getStringList("worldeditor_gamerules_integer", player).stream()
                    .map(line -> line.replace("%value%", world.getGameRuleValue(gameRule).toString()))
                    .toList();
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

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GameRulesInventoryHolder holder)) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = holder.getBuildWorld();

        switch (XMaterial.matchXMaterial(event.getCurrentItem())) {
            case PLAYER_HEAD:
                boolean pageChanged = false;
                int slot = event.getSlot();
                if (slot == 36) {
                    pageChanged = decrementInv(player, this.numGameRules, SLOTS.length);
                } else if (slot == 44) {
                    pageChanged = incrementInv(player, this.numGameRules, SLOTS.length);
                }
                if (!pageChanged) {
                    return;
                }
                break;

            case FILLED_MAP:
            case MAP:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                modifyGameRule(event, buildWorld.getWorld());
                break;

            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                new EditInventory(plugin).openInventory(player, buildWorld);
                return;
        }

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
        if (gameRule == null) {
            plugin.getLogger().warning("GameRule '%s' does not exist in world '%s'.".formatted(rawName, world.getName()));
            return;
        }

        if (isOfType(gameRule, Boolean.class)) {
            GameRule<Boolean> booleanRule = castRule(gameRule, Boolean.class);
            boolean currentValue = Boolean.TRUE.equals(world.getGameRuleValue(booleanRule));
            world.setGameRule(booleanRule, !currentValue);
        } else if (isOfType(gameRule, Integer.class)) {
            GameRule<Integer> integerRule = castRule(gameRule, Integer.class);
            int value = world.getGameRuleValue(integerRule);
            int delta = event.isShiftClick()
                    ? (event.isRightClick() ? 10 : event.isLeftClick() ? -10 : 0)
                    : (event.isRightClick() ? 1 : event.isLeftClick() ? -1 : 0);
            world.setGameRule(integerRule, value + delta);
        } else {
            plugin.getLogger().warning("GameRule '%s' is not a boolean or integer type and cannot be modified.".formatted(gameRule.getName()));
        }
    }

    /**
     * Casts a {@link GameRule} to the specified type if it matches.
     *
     * @param rule The game rule to cast
     * @param type The type to cast the game rule to
     * @param <T>  The type to cast the game rule to
     * @return The casted game rule if it matches the type, or {@code null} if it does not match
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> GameRule<T> castRule(GameRule<?> rule, Class<T> type) {
        return type.equals(rule.getType()) ? (GameRule<T>) rule : null;
    }

    /**
     * Gets whether the given {@link GameRule} is of the given type.
     *
     * @param gameRule The game rule to check
     * @param type     The type to check against
     * @param <T>      The type to check against
     * @return {@code true} if the game rule is of the given type, {@code false} otherwise
     */
    private static <T> boolean isOfType(@Nullable GameRule<?> gameRule, Class<T> type) {
        return gameRule != null && Objects.equals(gameRule.getType(), type);
    }

    /**
     * Checks if the {@link GameRule} is enabled in the given {@link World}.
     * <p>
     * If a game rule is not a boolean type, it is considered enabled by default.
     *
     * @param world    The world to check the game rule in
     * @param gameRule The game rule to check
     * @return {@code true} if the game rule is enabled, {@code false} otherwise
     */
    private boolean isEnabled(World world, GameRule<?> gameRule) {
        GameRule<Boolean> booleanGameRule = castRule(gameRule, Boolean.class);
        if (booleanGameRule != null) {
            return Boolean.TRUE.equals(world.getGameRuleValue(booleanGameRule));
        }
        return true;
    }

    private static class GameRulesInventoryHolder extends BuildWorldHolder {

        public GameRulesInventoryHolder(BuildWorld buildWorld, Player player) {
            super(buildWorld, 45, Messages.getString("worldeditor_gamerules_title", player));
        }
    }
}