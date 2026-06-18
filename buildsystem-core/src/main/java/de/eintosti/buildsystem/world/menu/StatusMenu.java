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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Lets a player assign one of the registered {@link BuildWorldStatus statuses} to a world. The grid is built dynamically
 * from the {@code WorldStatusRegistry}, so custom statuses appear automatically; each status renders with its own icon
 * and coloured name and is gated by its own permission.
 */
@NullMarked
public class StatusMenu extends ButtonMenu<MenuButton> {

    private static final int STATUSES_PER_ROW = 7;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 6;

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    public StatusMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(
                plugin.getMessages(),
                computeSize(plugin),
                plugin.getMessages()
                        .getString("status_title", player, Map.entry("%world%", formatWorldName(buildWorld))));
        this.plugin = plugin;
        this.buildWorld = buildWorld;

        List<BuildWorldStatus> statuses =
                List.copyOf(plugin.getWorldStatusRegistry().getStatuses());
        // Lay the grid out row by row, keeping a one-slot border on every side. The six-row chest bounds capacity, so
        // guard against placing a button outside the inventory (which would throw) when an admin has created a very
        // large number of statuses.
        int capacity = (rowCount(statuses.size()) - 2) * STATUSES_PER_ROW;
        for (int index = 0; index < statuses.size() && index < capacity; index++) {
            int slot = (index / STATUSES_PER_ROW + 1) * 9 + 1 + index % STATUSES_PER_ROW;
            register(slot, statusButton(statuses.get(index)));
        }
        if (statuses.size() > capacity) {
            plugin.getLogger()
                    .warning(
                            "StatusMenu can display at most %d statuses; %d are hidden. Reduce the number of statuses to reach them all."
                                    .formatted(capacity, statuses.size() - capacity));
        }
    }

    /**
     * Sizes the inventory to a whole number of rows large enough to hold the status grid with a one-slot border on each
     * side, clamped to the chest maximum of six rows.
     */
    private static int computeSize(BuildSystemPlugin plugin) {
        return rowCount(plugin.getWorldStatusRegistry().getStatuses().size()) * 9;
    }

    /**
     * The inventory row count: one content row per {@value #STATUSES_PER_ROW} statuses plus a top and bottom border row,
     * clamped between {@value #MIN_ROWS} and the six-row chest maximum.
     */
    private static int rowCount(int statusCount) {
        int contentRows = Math.max(1, (int) Math.ceil(statusCount / (double) STATUSES_PER_ROW));
        return Math.clamp(contentRows + 2, MIN_ROWS, MAX_ROWS);
    }

    private static String formatWorldName(BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        if (worldName.length() > 17) {
            worldName = worldName.substring(0, 14) + "...";
        }
        return worldName;
    }

    private MenuButton statusButton(BuildWorldStatus status) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    XMaterial material = status.getIcon();
                    String displayName = ColorAPI.process(status.getStyledName());

                    if (!player.hasPermission(status.getPermission())) {
                        material = XMaterial.BARRIER;
                        displayName = "§c§m" + ChatColor.stripColor(displayName);
                    }

                    ItemBuilder.of(material)
                            .name(displayName)
                            .glow(buildWorld.getData().getStatus().equals(status))
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    if (!player.hasPermission(status.getPermission())) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        return;
                    }

                    player.closeInventory();
                    buildWorld.getData().setStatus(status);
                    plugin.getSettingsService().forceUpdateSidebar(buildWorld);

                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    messages.sendMessage(
                            player,
                            "worlds_setstatus_set",
                            Map.entry("%world%", buildWorld.getName()),
                            Map.entry("%status%", ColorAPI.process(status.getStyledName())));
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillAll(player, getInventory());
        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Only react to clicks inside this menu (not the player's own inventory).
        if (event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        // A click on any non-status slot (e.g. the filler) returns to the editor.
        XSound.BLOCK_CHEST_OPEN.play(player);
        new EditMenu(plugin, buildWorld, player).open(player);
    }
}
