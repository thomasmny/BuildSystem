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
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class StatusMenu extends ButtonMenu<MenuButton> {

    /**
     * The single source of truth for the status selection grid: each interactive slot maps to the status it selects.
     * Slot 15 is intentionally absent (a gap in the layout). Drives both {@link #populate} and {@link #handleClick}.
     */
    private static final Map<Integer, BuildWorldStatus> STATUS_BY_SLOT = Map.ofEntries(
            Map.entry(10, BuildWorldStatus.NOT_STARTED),
            Map.entry(11, BuildWorldStatus.IN_PROGRESS),
            Map.entry(12, BuildWorldStatus.ALMOST_FINISHED),
            Map.entry(13, BuildWorldStatus.FINISHED),
            Map.entry(14, BuildWorldStatus.ARCHIVE),
            Map.entry(16, BuildWorldStatus.HIDDEN));

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    public StatusMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(
                plugin.getMessages(),
                27,
                plugin.getMessages()
                        .getString("status_title", player, Map.entry("%world%", formatWorldName(buildWorld))));
        this.plugin = plugin;
        this.buildWorld = buildWorld;

        STATUS_BY_SLOT.forEach((slot, status) -> register(slot, statusButton(status)));
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
                    XMaterial material = plugin.getCustomizableIcons().getIcon(status);
                    String displayName = messages.getString(Messages.getMessageKey(status), player);

                    if (!player.hasPermission(status.getPermission())) {
                        material = XMaterial.BARRIER;
                        displayName = "§c§m" + ChatColor.stripColor(displayName);
                    }

                    ItemBuilder.of(material)
                            .name(displayName)
                            .glow(buildWorld.getData().getStatus() == status)
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
                            Map.entry("%status%", messages.getString(Messages.getMessageKey(status), player)));
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillRange(player, getInventory(), 0, 10);
        plugin.getMenuItems().fillRange(player, getInventory(), 17, 27);

        renderButtons(player);
    }

    /**
     * The slot &rarr; status mapping. Exposed for the golden test that pins the selection grid.
     */
    Map<Integer, BuildWorldStatus> statusBySlot() {
        return STATUS_BY_SLOT;
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
