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

    private static final int FIRST_STATUS_SLOT = 10;

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
        int slot = FIRST_STATUS_SLOT;
        for (BuildWorldStatus status : statuses) {
            register(slot++, statusButton(status));
        }
    }

    /**
     * Sizes the inventory to a whole number of rows large enough to hold the status grid with a one-slot border on each
     * side, clamped to the chest maximum of six rows.
     */
    private static int computeSize(BuildSystemPlugin plugin) {
        int count = plugin.getWorldStatusRegistry().getStatuses().size();
        int rows = Math.min(6, Math.max(3, 2 + (int) Math.ceil((count + 1) / 7.0)));
        return rows * 9;
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
