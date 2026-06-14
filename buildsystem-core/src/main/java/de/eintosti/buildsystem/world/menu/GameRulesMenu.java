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
package de.eintosti.buildsystem.world.menu;

import com.cryptomorin.xseries.XGameRule;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class GameRulesMenu extends PaginatedMenu {

    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private static final int ITEMS_PER_PAGE = SLOTS.length;

    private static final int MENU_SIZE = 45;
    private static final int SLOT_PREVIOUS_PAGE = 36;
    private static final int SLOT_NEXT_PAGE = 44;

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    public GameRulesMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("worldeditor_gamerules_title", player));
        this.plugin = plugin;
        this.buildWorld = buildWorld;
    }

    @Override
    protected int totalItems() {
        return buildWorld.getWorld().map(world -> world.getGameRules().length).orElse(0);
    }

    @Override
    protected void populate(Player player) {
        Optional<World> optionalWorld = buildWorld.getWorld();
        if (optionalWorld.isEmpty()) {
            player.closeInventory();
            plugin.getLogger().severe("World '" + buildWorld.getName() + "' does not exist.");
            return;
        }
        World world = optionalWorld.get();

        for (int i = 0; i < MENU_SIZE; i++) {
            if (!isValidSlot(i)) {
                plugin.getMenuItems().addGlassPane(player, getInventory(), i);
            }
        }

        addPageNavItem(player, SLOT_PREVIOUS_PAGE, page() > 0, "gui_previous_page", SkullTextures.PREVIOUS_PAGE);
        addPageNavItem(
                player,
                SLOT_NEXT_PAGE,
                page() < totalPages(ITEMS_PER_PAGE) - 1,
                "gui_next_page",
                SkullTextures.NEXT_PAGE);

        String[] allGameRules = world.getGameRules();
        int startIndex = page() * ITEMS_PER_PAGE;
        for (int i = 0; i < SLOTS.length && startIndex + i < allGameRules.length; i++) {
            addGameRuleItem(SLOTS[i], world, allGameRules[startIndex + i], player);
        }
    }

    /**
     * Renders a page-navigation skull when more pages exist in the given direction, otherwise a filler glass pane.
     *
     * @param player The viewing player
     * @param slot The slot to render into
     * @param hasMorePages Whether a page exists in this direction (and there is more than one page)
     * @param nameKey The message key for the skull's display name
     * @param skullTexture The skull texture to use
     */
    private void addPageNavItem(Player player, int slot, boolean hasMorePages, String nameKey, String skullTexture) {
        if (totalPages(ITEMS_PER_PAGE) > 1 && hasMorePages) {
            ItemBuilder.skull(Profileable.detect(skullTexture))
                    .name(messages.getString(nameKey, player))
                    .into(getInventory(), slot);
        } else {
            plugin.getMenuItems().addGlassPane(player, getInventory(), slot);
        }
    }

    private void addGameRuleItem(int slot, World world, String gameRuleName, Player player) {
        XGameRule<?> gameRule = XGameRule.of(gameRuleName).orElse(null);
        if (gameRule == null || !gameRule.isSupported()) {
            plugin.getLogger()
                    .severe("GameRule '" + gameRuleName + "' does not exist in world '" + world.getName() + "'.");
            return;
        }

        ItemBuilder.of(isEnabled(world, gameRule) ? XMaterial.FILLED_MAP : XMaterial.MAP)
                .name(ChatColor.YELLOW + gameRule.name())
                .lore(getLore(world, gameRule, player))
                .into(getInventory(), slot);
    }

    private List<String> getLore(World world, XGameRule<?> gameRule, Player player) {
        List<String> lore;
        if (isOfType(gameRule, Boolean.class)) {
            lore = isEnabled(world, gameRule)
                    ? messages.getStringList("worldeditor_gamerules_boolean_enabled", player)
                    : messages.getStringList("worldeditor_gamerules_boolean_disabled", player);
        } else {
            lore = messages.getStringList("worldeditor_gamerules_integer", player).stream()
                    .map(line -> line.replace("%value%", String.valueOf(gameRule.getValue(world))))
                    .toList();
        }
        return lore;
    }

    public boolean isValidSlot(int slot) {
        return Arrays.stream(SLOTS).anyMatch(i -> i == slot);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        switch (XMaterial.matchXMaterial(event.getCurrentItem())) {
            case PLAYER_HEAD:
                boolean pageChanged = false;
                int slot = event.getSlot();
                if (slot == SLOT_PREVIOUS_PAGE) {
                    pageChanged = previousPage(player, ITEMS_PER_PAGE);
                } else if (slot == SLOT_NEXT_PAGE) {
                    pageChanged = nextPage(player, ITEMS_PER_PAGE);
                }
                if (!pageChanged) {
                    return;
                }
                populate(player);
                return;

            case FILLED_MAP:
            case MAP:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                modifyGameRule(event, buildWorld.getWorld().orElse(null));
                populate(player);
                return;

            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                new EditMenu(plugin, buildWorld, player).open(player);
        }
    }

    private void modifyGameRule(InventoryClickEvent event, @Nullable World world) {
        if (world == null) {
            return;
        }

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
        XGameRule<?> gameRule = XGameRule.of(rawName).orElse(null);
        if (gameRule == null || !gameRule.isSupported()) {
            plugin.getLogger()
                    .warning("GameRule '%s' does not exist in world '%s'.".formatted(rawName, world.getName()));
            return;
        }

        if (isOfType(gameRule, Boolean.class)) {
            XGameRule<Boolean> booleanRule = castRule(gameRule, Boolean.class);
            boolean currentValue = Boolean.TRUE.equals(booleanRule.getValue(world));
            booleanRule.setValue(world, !currentValue);
        } else if (isOfType(gameRule, Integer.class)) {
            XGameRule<Integer> integerRule = castRule(gameRule, Integer.class);
            int value = integerRule.getValue(world);
            int delta = event.isShiftClick()
                    ? (event.isRightClick() ? 10 : event.isLeftClick() ? -10 : 0)
                    : (event.isRightClick() ? 1 : event.isLeftClick() ? -1 : 0);
            integerRule.setValue(world, value + delta);
        } else {
            plugin.getLogger()
                    .warning("GameRule '%s' is not a boolean or integer type and cannot be modified."
                            .formatted(gameRule.name()));
        }
    }

    /**
     * Casts an {@link XGameRule} to the specified type if it matches.
     *
     * @param rule The game rule to cast
     * @param type The type to cast the game rule to
     * @param <T> The type to cast the game rule to
     * @return The casted game rule if it matches the type, or {@code null} if it does not match
     */
    @SuppressWarnings("unchecked")
    @Nullable @Contract("_, _ -> _")
    private static <T> XGameRule<T> castRule(XGameRule<?> rule, Class<T> type) {
        return type.equals(rule.getType()) ? (XGameRule<T>) rule : null;
    }

    /**
     * Gets whether the given {@link XGameRule} is of the given type.
     *
     * @param gameRule The game rule to check
     * @param type The type to check against
     * @param <T> The type to check against
     * @return {@code true} if the game rule is of the given type, {@code false} otherwise
     */
    private static <T> boolean isOfType(@Nullable XGameRule<?> gameRule, Class<T> type) {
        return gameRule != null && Objects.equals(gameRule.getType(), type);
    }

    /**
     * Checks if the {@link XGameRule} is enabled in the given {@link World}.
     *
     * <p>If a game rule is not a boolean type, it is considered enabled by default.
     *
     * @param world The world to check the game rule in
     * @param gameRule The game rule to check
     * @return {@code true} if the game rule is enabled, {@code false} otherwise
     */
    private boolean isEnabled(World world, XGameRule<?> gameRule) {
        XGameRule<Boolean> booleanGameRule = castRule(gameRule, Boolean.class);
        if (booleanGameRule != null) {
            return Boolean.TRUE.equals(booleanGameRule.getValue(world));
        }
        return true;
    }
}
